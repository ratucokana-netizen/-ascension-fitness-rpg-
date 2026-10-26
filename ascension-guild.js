/* Ascension Guild: opt-in online friends leaderboard via Supabase Auth and REST.
   Requires a project URL, publishable/anon key and guild-schema.sql installed.
   Never put service-role keys in this client. */
(function(){
'use strict';
const $=id=>document.getElementById(id);
const cfg=()=>{try{return {url:'https://hvcpguioxhxubgbyenfn.supabase.co',...JSON.parse(localStorage.getItem('ascension_guild_config')||'{}')}}catch{return {url:'https://hvcpguioxhxubgbyenfn.supabase.co'}}};
const saveCfg=v=>localStorage.setItem('ascension_guild_config',JSON.stringify(v));
const getSession=()=>{try{return JSON.parse(localStorage.getItem('ascension_guild_session')||'null')}catch{return null}};
const setSession=s=>localStorage.setItem('ascension_guild_session',JSON.stringify(s));
const escape=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const uid=()=>getSession()?.user?.id;
function score(){const v=state.v2||{},sessions=v.sessions||[];return {xp:xp()+sessions.reduce((n,s)=>n+Math.min(300,100+(s.exercises||[]).reduce((a,e)=>a+(e.sets||[]).filter(t=>t.done&&!t.warmup).length*15,0)),0),sessions:sessions.length};}
function endpoint(){const c=cfg();if(!/^https:\/\/[a-z0-9-]+\.supabase\.co$/i.test(c.url||'')||!/^[-_A-Za-z0-9.]+$/.test(c.key||''))throw Error('Connect a valid Supabase project in Guild settings.');return c;}
async function api(path,options={}){
 const c=endpoint(),sess=getSession(),token=sess?.access_token||c.key;
 const res=await fetch(c.url+'/rest/v1/'+path,{...options,headers:{apikey:c.key,Authorization:'Bearer '+token,'Content-Type':'application/json',Prefer:'return=representation',...(options.headers||{})}});
 const body=await res.text();let data;try{data=body?JSON.parse(body):null}catch{data=body}
 if(!res.ok)throw Error(data?.message||data?.error_description||'Online request failed ('+res.status+')');
 return data;
}
async function auth(path,body){const c=endpoint();const r=await fetch(c.url+'/auth/v1/'+path,{method:'POST',headers:{apikey:c.key,'Content-Type':'application/json'},body:JSON.stringify(body)});const d=await r.json();if(!r.ok)throw Error(d.msg||d.error_description||d.message||'Authentication failed');return d;}
function notice(message){const el=$('guild-status');if(el)el.textContent=message}
async function sync(){
 if(!uid())return;
 const sc=score();const name=(state.name||'HUNTER').replace(/[^a-z0-9 _-]/gi,'').slice(0,20);
 await api('ascension_profiles?on_conflict=user_id',{method:'POST',headers:{Prefer:'resolution=merge-duplicates,return=representation'},body:JSON.stringify({user_id:uid(),display_name:name||'HUNTER',xp:sc.xp,sessions:sc.sessions,training_class:state.trainingClass||'warden',updated_at:new Date().toISOString()})});
}
async function rows(){const id=uid();if(!id)return [];return api('ascension_leaderboard?select=*&order=xp.desc&limit=100');}
async function friends(){return api('ascension_friendships?select=*&limit=100')}
function ranking(list){const mine=uid();return '<div class="guild-ranks">'+(list.length?list.map((r,i)=>'<div class="guild-rank '+(r.user_id===mine?'guild-me':'')+'"><strong class="guild-place">'+String(i+1).padStart(2,'0')+'</strong><span class="guild-name">'+escape(r.display_name)+'<small>'+escape((r.training_class||'warden').toUpperCase())+' · '+Number(r.sessions||0)+' SESSIONS</small></span><strong class="guild-points">'+Number(r.xp||0).toLocaleString()+' XP</strong></div>').join(''):'<p>No rankings yet. Invite a friend to begin.</p>')+'</div>'}
function shell(){return '<div class="guild-screen"><div class="guild-heading"><span>ASCENSION NETWORK // GUILD PROTOCOL</span><h1>HUNTER <em>RANKINGS</em></h1><p>Compete with friends using XP earned from logged training. Rankings are private to accepted connections.</p></div><div id="guild-status" role="status" class="guild-status">Awaiting network connection</div><div id="guild-content"></div></div>'}
function disconnected(){
 const c=cfg();return '<div class="guild-panel"><h2>CONNECT ONLINE LEADERBOARD</h2><p>To enable live rankings and invitations, connect your Supabase project. This is a one-time setup; do not use a service-role secret.</p><label>Project URL<input id="guild-url" type="url" placeholder="https://your-project.supabase.co" value="'+escape(c.url||'')+'"></label><label>Publishable / anon key<input id="guild-key" type="password" placeholder="sb_publishable_... or anon JWT" value="'+escape(c.key||'')+'"></label><button id="guild-connect" type="button">SAVE CONNECTION</button></div>'}
function login(){return '<div class="guild-panel"><h2>JOIN THE GUILD</h2><p>Sign in with your email to securely share training rankings with friends.</p><label>Email<input id="guild-email" type="email" autocomplete="email" placeholder="you@example.com"></label><button id="guild-send" type="button">SEND EMAIL CODE</button><label>6-digit verification code<input id="guild-code" inputmode="numeric" autocomplete="one-time-code" placeholder="000000"></label><button id="guild-verify" type="button">VERIFY & CONNECT</button></div>'}
function connected(){return '<div class="guild-panel"><div class="guild-actions"><button id="guild-sync" type="button">SYNC MY XP</button><button id="guild-refresh" type="button">REFRESH RANKINGS</button><button id="guild-logout" type="button">SIGN OUT</button></div><div class="guild-invite"><label>Friend email (must have joined Ascension)<input id="guild-friend-email" type="email" placeholder="friend@example.com"></label><button id="guild-invite" type="button">SEND FRIEND INVITE</button></div><h2>FRIENDS LEADERBOARD</h2><div id="guild-board">Loading...</div><h2>PENDING INVITES</h2><div id="guild-pending">Loading...</div></div>'}
async function refresh(){
 const el=$('guild-board'),pending=$('guild-pending');if(!el)return;
 try{await sync();const [rank,links]=await Promise.all([rows(),friends()]);el.innerHTML=ranking(rank);const requests=links.filter(f=>f.recipient_id===uid()&&f.status==='pending');pending.innerHTML=requests.length?requests.map(f=>'<div class="guild-request">Friend request <button data-accept="'+escape(f.id)+'" type="button">ACCEPT</button></div>').join(''):'<p>No pending invites</p>';pending.querySelectorAll('[data-accept]').forEach(b=>b.onclick=async()=>{try{await api('ascension_friendships?id=eq.'+encodeURIComponent(b.dataset.accept),{method:'PATCH',body:JSON.stringify({status:'accepted'})});notice('Friend added.');refresh()}catch(e){notice(e.message)}});notice('Leaderboard updated.')}catch(e){notice(e.message);el.textContent='Unable to load rankings.'}
}
function bind(){
 const connect=$('guild-connect');if(connect)connect.onclick=()=>{saveCfg({url:$('guild-url').value.trim().replace(/\/$/,''),key:$('guild-key').value.trim()});render()};
 const send=$('guild-send');if(send)send.onclick=async()=>{try{const email=$('guild-email').value.trim();if(!email)throw Error('Enter your email.');sessionStorage.setItem('guild_email',email);await auth('otp',{email,create_user:true});notice('Verification email sent.')}catch(e){notice(e.message)}};
 const verify=$('guild-verify');if(verify)verify.onclick=async()=>{try{const email=$('guild-email').value.trim()||sessionStorage.getItem('guild_email');const d=await auth('verify',{email,token:$('guild-code').value.trim(),type:'email'});if(!d.access_token)throw Error('No session returned.');setSession(d);render()}catch(e){notice(e.message)}};
 const syncBtn=$('guild-sync');if(syncBtn)syncBtn.onclick=refresh;
 const refreshBtn=$('guild-refresh');if(refreshBtn)refreshBtn.onclick=refresh;
 const logout=$('guild-logout');if(logout)logout.onclick=()=>{localStorage.removeItem('ascension_guild_session');render()};
 const invite=$('guild-invite');if(invite)invite.onclick=async()=>{try{const email=$('guild-friend-email').value.trim().toLowerCase();if(!email)throw Error('Enter your friend’s email.');await api('rpc/ascension_invite_friend',{method:'POST',body:JSON.stringify({friend_email:email})});notice('Friend request sent. They can accept in their Guild tab.');refresh()}catch(e){notice(e.message)}};
}
function render(){
 const host=$('guild');if(!host)return;host.innerHTML=shell();const c=cfg();const ready=!!(c.url&&c.key);$('guild-content').innerHTML=!ready?disconnected():!uid()?login():connected();bind();if(ready&&uid())refresh();else notice(ready?'Secure sign-in required.':'Online service not connected.');
}
window.AscensionGuild={render,sync,score};
})();