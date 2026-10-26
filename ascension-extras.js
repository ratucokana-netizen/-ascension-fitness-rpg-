/* Ascension cutscenes + camera-assisted food entry. No invented nutrition estimates. */
(function(){
'use strict';
const $=id=>document.getElementById(id);
const esc=x=>String(x??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const KEYS=['calories','protein','carbs','fat'];
let queue=[],playing=false,photoURL=null,photoFile=null,initialized=false;
function points(){return window.AscensionShadow?.current?.()||null}
function show(item){
 queue.push(item);if(playing)return;next();
}
function next(){
 if(!queue.length){playing=false;return}
 playing=true;const item=queue.shift(),d=points();
 let layer=$('ascension-cutscene');
 if(!layer){layer=document.createElement('div');layer.id='ascension-cutscene';layer.setAttribute('role','dialog');layer.setAttribute('aria-modal','true');layer.setAttribute('aria-label','Achievement unlocked');document.body.appendChild(layer)}
 const art=$('dashboard')?.querySelector('.shade-svg')?.outerHTML||'';
 layer.innerHTML='<div class="cutscene-notice">SYSTEM NOTIFICATION <span>◆ ACHIEVEMENT UNLOCKED</span></div><div class="cutscene-content"><div class="cutscene-burst"></div><div class="cutscene-art">'+art+'</div><div class="cutscene-eyebrow">ASCENSION // PROGRESSION</div><h2>'+esc(item.title)+'</h2><p>'+esc(item.detail)+'</p><div class="cutscene-xp"><strong>+'+esc(item.xp||0)+' XP</strong><span>COMPANION ENERGY INCREASED</span></div><div class="cutscene-meter"><span></span></div><button id="cutscene-close" type="button">CONTINUE</button></div>';
 layer.classList.add('visible');
 const close=()=>{layer.classList.remove('visible');next()};
 $('cutscene-close').onclick=close;
 layer.onkeydown=e=>{if(e.key==='Escape')close()};
 $('cutscene-close').focus();
}
function achievements(){
 if(!window.AscensionV2?.achievements)return;
 const earned=window.AscensionV2.achievements().filter(x=>x.met);
 const key='ascension_cutscene_seen_v1';let seen=[];
 try{seen=JSON.parse(localStorage.getItem(key)||'[]')}catch{}
 if(!initialized){initialized=true; // On first install, do not replay previously earned achievements.
   if(localStorage.getItem(key)===null){localStorage.setItem(key,JSON.stringify(earned.map(x=>x.name)));return}
 }
 const unseen=earned.filter(x=>!seen.includes(x.name));
 if(unseen.length){localStorage.setItem(key,JSON.stringify([...new Set([...seen,...unseen.map(x=>x.name)])]));unseen.forEach(x=>show({title:x.name,detail:x.detail,xp:0}))}
 const d=points();if(!d)return;
 const lkey='ascension_last_companion_level';
 const prior=Number(localStorage.getItem(lkey)||d.level);
 if(d.level>prior){show({title:'LEVEL '+d.level+' REACHED',detail:'Your training has strengthened your companion.',xp:(d.level-prior)*500})}
 localStorage.setItem(lkey,String(d.level));
}
function cameraPanel(){
 const host=$('metrics');if(!host)return;
 if($('ascension-camera-panel'))return;
 const section=document.createElement('section');section.id='ascension-camera-panel';section.className='panel camera-panel';
 section.innerHTML='<div class="panel-inner"><div class="section-head"><h2>CAMERA FOOD LOG</h2><span class="tag">CAMERA ASSIST</span></div><p class="hero-copy">Take a photo of a nutrition label or meal, then enter the label values below. Photos stay on your device and are not uploaded. Use AI to estimate macros from a meal photo or read a nutrition label. Estimates must be reviewed before saving. Images are sent securely to the analysis service only when you tap Analyze.</p><label class="action-btn primary camera-trigger" for="ascension-food-camera">OPEN CAMERA / SELECT PHOTO</label><input type="file" id="ascension-food-camera" accept="image/*" capture="environment" aria-label="Take food or nutrition label photo"><div id="ascension-photo-preview" aria-live="polite"></div><div class="camera-entry"><label>Food description<input id="ascension-food-name" placeholder="e.g. Greek yogurt, 200 g" maxlength="100"></label>'+KEYS.map(k=>'<label>'+k.toUpperCase()+(k==='calories'?' (kcal)':' (g)')+'<input id="ascension-food-'+k+'" type="number" inputmode="decimal" min="0" max="10000" step="any" placeholder="0"></label>').join('')+'</div><button class="action-btn primary" id="ascension-add-food" type="button">ADD TO TODAY’S MACROS</button><p id="ascension-food-status" role="status" class="hero-copy"></p></div>';
 const target=host.querySelector('.grid-2');if(target)target.insertAdjacentElement('afterend',section);else host.appendChild(section);
 const input=$('ascension-food-camera');
 input.onchange=()=>{
 const file=input.files?.[0];if(!file)return;
 if(!file.type.startsWith('image/')){$('ascension-food-status').textContent='Select an image file.';return}
 if(file.size>12*1024*1024){$('ascension-food-status').textContent='Photo too large. Choose an image under 12 MB.';return}
 if(photoURL)URL.revokeObjectURL(photoURL);photoFile=file;photoURL=URL.createObjectURL(file);
 const preview=$('ascension-photo-preview');preview.textContent='';const img=document.createElement('img');img.src=photoURL;img.alt='Food or nutrition label photo preview';preview.appendChild(img);
 $('ascension-food-status').textContent='Photo ready. Tap Analyze Photo with AI for an estimate, or enter label values manually.';
 };

 $('ascension-analyze-food').onclick=async()=>{
  const status=$('ascension-food-status'),review=$('ascension-ai-review'),button=$('ascension-analyze-food');
  if(!photoFile){status.textContent='Take or select a food photo first.';return}
  let config,session;try{config=JSON.parse(localStorage.getItem('ascension_guild_config')||'{}');session=JSON.parse(localStorage.getItem('ascension_guild_session')||'null')}catch{}
  if(!config?.url||!config?.key||!session?.access_token){status.textContent='Connect your Supabase project and sign in through Guild to enable AI photo analysis.';return}
  if(!/^https:\/\/[a-z0-9-]+\.supabase\.co$/i.test(config.url)){status.textContent='Invalid Supabase project URL.';return}
  button.disabled=true;button.textContent='ANALYZING PHOTO...';status.textContent='Analyzing securely. Review all estimates before saving.';
  try{
   const image=await new Promise((resolve,reject)=>{
    const img=new Image();const url=URL.createObjectURL(photoFile);
    img.onload=()=>{try{const max=1280,scale=Math.min(1,max/Math.max(img.naturalWidth,img.naturalHeight)),canvas=document.createElement('canvas');canvas.width=Math.max(1,Math.round(img.naturalWidth*scale));canvas.height=Math.max(1,Math.round(img.naturalHeight*scale));canvas.getContext('2d').drawImage(img,0,0,canvas.width,canvas.height);const out=canvas.toDataURL('image/jpeg',.78);URL.revokeObjectURL(url);resolve(out)}catch(e){URL.revokeObjectURL(url);reject(e)}};
    img.onerror=()=>{URL.revokeObjectURL(url);reject(Error('Could not read photo.'))};img.src=url;
   });
   const response=await fetch(config.url+'/functions/v1/analyze-food',{method:'POST',headers:{apikey:config.key,Authorization:'Bearer '+session.access_token,'Content-Type':'application/json'},body:JSON.stringify({image})});
   const payload=await response.json();if(!response.ok)throw Error(payload.error||'Image analysis failed');
   const result=payload.result;if(!result||result.kind==='unknown')throw Error('No recognizable food or nutrition label. Enter values manually.');
   $('ascension-food-name').value=String(result.food_name||'').slice(0,100);
   for(const k of KEYS){const n=result[k];$('ascension-food-'+k).value=typeof n==='number'&&Number.isFinite(n)&&n>=0?String(Math.round(n*10)/10):''}
   review.textContent='AI '+result.kind+' estimate · '+result.confidence+' confidence · '+(result.serving_description||'Portion unclear')+'. '+(result.assumptions||[]).join(' ')+' Review the portion and all four macros before saving.';
   status.textContent='Estimate ready. Confirm or correct the values, then tap Add to Today’s Macros.';
  }catch(e){status.textContent=e.message||'Analysis unavailable. Enter macros manually.'}
  finally{button.disabled=false;button.textContent='ANALYZE PHOTO WITH AI'}
 };
 $('ascension-add-food').onclick=()=>{
 const name=$('ascension-food-name').value.trim();
 if(!name){$('ascension-food-status').textContent='Enter a food description first.';return}
 const amounts={};for(const k of KEYS){const el=$('ascension-food-'+k);if(el.value.trim()===''||!Number.isFinite(Number(el.value))||Number(el.value)<0||Number(el.value)>10000){$('ascension-food-status').textContent='Enter a valid '+k+' amount.';return}amounts[k]=Number(el.value)}
 const date=new Date(),day=[date.getFullYear(),String(date.getMonth()+1).padStart(2,'0'),String(date.getDate()).padStart(2,'0')].join('-');
 state.v2 ||= {};state.v2.nutrition ||= {};const existing=state.v2.nutrition[day]||{};
 state.v2.nutrition[day]=Object.fromEntries(KEYS.map(k=>[k,Math.round((Number(existing[k])||0)+amounts[k])]));
 state.v2.foodEntries ||= {};state.v2.foodEntries[day] ||= [];state.v2.foodEntries[day].push({name,amounts,loggedAt:new Date().toISOString()});
 if(typeof save==='function')save();else localStorage.setItem('ascension_v1',JSON.stringify(state));
 if(window.AscensionV2?.render)window.AscensionV2.render();
 $('ascension-food-status') && ($('ascension-food-status').textContent='Food added to today’s totals.');
 };
}
function render(){achievements();cameraPanel()}
window.AscensionExtras={render,notify:show};
})();