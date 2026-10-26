/* ASCENSION: original, procedural evolving shadow companion.
   All forms are drawn in SVG/CSS; no third-party characters or art. */
(function(){
'use strict';
const $=id=>document.getElementById(id);
const PALETTES={
vanguard:['#64dfff','#4266ff','Fortress'],
berserker:['#ff638d','#b63aff','Inferno'],
shadow:['#a581ff','#5351ff','Phantom'],
sentinel:['#6dffdc','#168fdf','Pulse'],
warden:['#d4b4ff','#697bff','Equilibrium']
};
const TITLES=['Ember','Wisp','Specter','Aegis'];
const STAGES=[1,11,31,61];
function clamp(n,min,max){return Math.min(max,Math.max(min,Number(n)||0))}
function current(){
 const v=state.v2||{},sessions=Array.isArray(v.sessions)?v.sessions:[],old=xp(),newXp=sessions.reduce((sum,s)=>sum+Math.min(300,100+(s.exercises||[]).reduce((total,e)=>total+(e.sets||[]).filter(t=>t.done&&!t.warmup).length*15,0)),0);
 const points=old+newXp,level=Math.floor(points/500)+1,stage=level>=61?3:level>=31?2:level>=11?1:0;
 const cls=PALETTES[state.trainingClass]?state.trainingClass:'warden';
 const [primary,secondary,form]=PALETTES[cls];
 const workingSets=sessions.reduce((sum,s)=>sum+(s.exercises||[]).reduce((a,e)=>a+(e.sets||[]).filter(t=>t.done&&!t.warmup).length,0),0);
 const dates=new Set(sessions.map(s=>s.date)).size;
 const evolutionReady=stage===0?true:(stage===1?dates>=3:stage===2?dates>=12&&workingSets>=50:dates>=30&&workingSets>=150);
 const actualStage=evolutionReady?stage:(stage===3?2:stage===2?1:0);
 return {cls,primary,secondary,form,level,points,stage:actualStage,desiredStage:stage,ready:evolutionReady,workingSets,dates,sessions:sessions.length};
}
function art(d,mini=false){
 const stage=d.stage;
 const scale=stage===0?.64:stage===1?.81:stage===2?1:1.14;
 const horns=stage>=2?'<path d="M103 105 Q73 60 85 29 Q106 53 125 67 M157 105 Q187 60 175 29 Q154 53 135 67" fill="none" stroke="url(#edge)" stroke-width="9"/>':'';
 const mantle=stage>=1?'<path d="M92 157 Q44 143 22 228 Q74 207 87 267 M168 157 Q216 143 238 228 Q186 207 173 267" fill="none" stroke="url(#edge)" stroke-width="'+(stage>=3?22:12)+'" opacity=".68"/>':'';
 const wings=stage>=3?'<path d="M87 170 Q14 112 12 40 Q64 64 106 136 M173 170 Q246 112 248 40 Q196 64 154 136" fill="none" stroke="url(#edge)" stroke-width="11" opacity=".65"/>':'';
 const armor=stage>=2?'<path d="M84 170 L108 151 L130 175 L152 151 L176 170 L166 223 L130 238 L94 223Z" fill="url(#armor)" stroke="url(#edge)" stroke-width="2"/><path d="M130 177 L119 205 L130 217 L141 205Z" fill="'+d.primary+'" opacity=".82"/>':'';
 const sigil=d.cls==='vanguard'?'<path d="M130 175 L151 188 L144 211 L130 220 L116 211 L109 188Z" fill="none" stroke="'+d.primary+'" stroke-width="3"/>':d.cls==='berserker'?'<path d="M130 170 L115 192 L128 192 L119 216 L148 185 L135 185Z" fill="'+d.primary+'"/>':d.cls==='shadow'?'<path d="M120 181 Q149 171 144 200 Q135 213 119 203 Q136 199 136 190 Q132 184 120 181Z" fill="'+d.primary+'"/>':d.cls==='sentinel'?'<circle cx="130" cy="193" r="13" stroke="'+d.primary+'" fill="none" stroke-width="3"/><path d="M130 177 V209 M114 193 H146" stroke="'+d.primary+'" stroke-width="2"/>':'<path d="M130 176 L145 191 L130 207 L115 191Z" fill="none" stroke="'+d.primary+'" stroke-width="3"/>';
 return '<svg class="shade-svg shade-stage-'+stage+'" viewBox="0 0 260 290" role="img" aria-label="'+d.form+' companion, '+TITLES[stage]+' form" xmlns="http://www.w3.org/2000/svg"><defs><radialGradient id="aura"><stop stop-color="'+d.primary+'" stop-opacity=".35"/><stop offset="1" stop-color="'+d.secondary+'" stop-opacity="0"/></radialGradient><linearGradient id="body" x2="0" y2="1"><stop stop-color="#1a2044"/><stop offset=".5" stop-color="#080b21"/><stop offset="1" stop-color="#030614"/></linearGradient><linearGradient id="edge" x2="0" y2="1"><stop stop-color="'+d.primary+'"/><stop offset="1" stop-color="'+d.secondary+'"/></linearGradient><linearGradient id="armor"><stop stop-color="#1b244e"/><stop offset="1" stop-color="#090f24"/></linearGradient><filter id="glow"><feGaussianBlur stdDeviation="4" result="b"/><feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge></filter></defs><ellipse cx="130" cy="158" rx="110" ry="130" fill="url(#aura)" class="shade-aura"/><g class="shade-float" transform="translate(130 148) scale('+scale+') translate(-130 -148)">'+wings+mantle+'<path d="M96 151 Q68 174 68 226 L80 277 L180 277 L192 226 Q192 174 164 151 L130 167Z" fill="url(#body)" stroke="url(#edge)" stroke-width="2"/><path d="M94 162 Q56 177 48 241 L78 249 L99 190 M166 162 Q204 177 212 241 L182 249 L161 190" fill="#080c22" stroke="url(#edge)" stroke-width="2"/>'+armor+'<path d="M91 101 Q93 54 130 50 Q167 54 169 101 L161 149 Q130 172 99 149Z" fill="url(#body)" stroke="url(#edge)" stroke-width="3"/>'+horns+'<path d="M104 113 L125 119 L116 125 L100 123Z M156 113 L135 119 L144 125 L160 123Z" fill="'+d.primary+'" filter="url(#glow)" class="shade-eyes"/>'+sigil+'<path d="M95 265 L81 281 M165 265 L179 281" stroke="url(#edge)" stroke-width="3"/></g><g class="shade-particles"><circle cx="38" cy="115" r="2" fill="'+d.primary+'"/><circle cx="213" cy="84" r="2.5" fill="'+d.primary+'"/><circle cx="205" cy="231" r="1.5" fill="'+d.primary+'"/><circle cx="51" cy="236" r="2" fill="'+d.primary+'"/></g></svg>';
}
function card(d,compact=false){
 const title=TITLES[d.stage],next=STAGES[d.stage+1],percent=Math.round((d.points%500)/5);
 return '<div class="shade-card '+(compact?'shade-compact':'')+'" style="--shade-primary:'+d.primary+';--shade-secondary:'+d.secondary+'"><div class="shade-art">'+art(d)+'</div><div class="shade-information"><div class="shade-overline">COMPANION // '+d.form.toUpperCase()+'</div><h2 class="shade-title">'+title+' <span>· LV '+d.level+'</span></h2><p class="shade-copy">'+(d.ready?'Bound to your training. Your progress shapes its next form.':'Evolution pending: train consistently to stabilize this form.')+'</p><div class="shade-bar"><span style="width:'+percent+'%"></span></div><div class="shade-meta"><span>'+percent+'% TO NEXT LEVEL</span><span>'+d.dates+' TRAINING DAYS</span></div>'+(compact?'':'<div class="shade-milestones">'+STAGES.map((l,i)=>'<span class="'+(d.stage>=i?'reached':'')+'">'+TITLES[i]+' · '+l+'+</span>').join('')+'</div>')+'</div></div>';
}
function render(){
 const d=current();
 const dashboard=$('dashboard');
 if(dashboard){let old=dashboard.querySelector('.shade-dashboard');if(old)old.remove();dashboard.insertAdjacentHTML('afterbegin','<div class="shade-dashboard">'+card(d)+'</div>');}
 const classes=$('classes');
 if(classes){let old=classes.querySelector('.shade-class-preview');if(old)old.remove();classes.insertAdjacentHTML('afterbegin','<div class="shade-class-preview">'+card(d,true)+'<div class="shade-guide">CLASS SHAPES FORM · TRAINING BUILDS POWER · CONSISTENCY UNLOCKS EVOLUTION</div></div>');}
 const avatar=$('systemAvatar');
 if(avatar){let core=avatar.querySelector('.system-core');if(core)core.style.display='none';let old=avatar.querySelector('.shade-avatar');if(old)old.remove();avatar.insertAdjacentHTML('afterbegin','<div class="shade-avatar" style="--shade-primary:'+d.primary+';--shade-secondary:'+d.secondary+'">'+art(d,true)+'</div>');avatar.dataset.class=d.cls;avatar.dataset.stage=String(d.stage);}
}
function speaking(active){const avatar=$('systemAvatar');if(avatar){avatar.classList.toggle('shade-speaking',!!active);avatar.dataset.state=active?'speaking':'idle';}}
window.AscensionShadow={render,speaking,current};
})();
