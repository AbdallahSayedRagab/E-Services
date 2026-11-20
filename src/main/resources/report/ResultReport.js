(function(){
var root=document.documentElement;
function isEn(){return root.lang==='en'}
var tip=document.getElementById('tip');
function show(el,x,y){
  tip.textContent='';
  var en=isEn(),d=(en&&el.dataset.de)||el.dataset.d;
  var b=document.createElement('b');b.textContent=(en&&el.dataset.te)||el.dataset.t;tip.appendChild(b);
  if(d){var s=document.createElement('span');s.textContent=d;tip.appendChild(s);}
  tip.style.opacity=1;
  var w=tip.offsetWidth,h=tip.offsetHeight;
  var l=Math.min(Math.max(8,x-w/2),window.innerWidth-w-8),t=y-h-12; if(t<8)t=y+18;
  tip.style.left=l+'px';tip.style.top=t+'px';
}
document.querySelectorAll('[data-t]').forEach(function(el){
  el.addEventListener('mousemove',function(e){show(el,e.clientX,e.clientY)});
  el.addEventListener('mouseleave',function(){tip.style.opacity=0});
  el.addEventListener('focus',function(){var r=el.getBoundingClientRect();show(el,r.left+r.width/2,r.top)});
  el.addEventListener('blur',function(){tip.style.opacity=0});
});
document.getElementById('theme').addEventListener('click',function(){
  var cur=root.getAttribute('data-theme');
  if(!cur) cur=matchMedia('(prefers-color-scheme: dark)').matches?'dark':'light';
  root.setAttribute('data-theme',cur==='dark'?'light':'dark');
});
document.querySelectorAll('.copy').forEach(function(b){
  var label=b.innerHTML;
  b.addEventListener('click',function(){
    var t=b.parentNode.querySelector('pre').textContent;
    var done=function(){b.textContent=isEn()?'Copied':'تم النسخ';setTimeout(function(){b.innerHTML=label},1500)};
    if(navigator.clipboard){navigator.clipboard.writeText(t).then(done,function(){})}
  });
});
document.querySelectorAll('.rcp').forEach(function(b){
  var label=b.innerHTML;
  b.addEventListener('click',function(){
    var done=function(){b.textContent=isEn()?'Copied':'تم النسخ';setTimeout(function(){b.innerHTML=label},1500)};
    if(navigator.clipboard){navigator.clipboard.writeText(b.dataset.v).then(done,function(){})}
  });
});
var tb=document.querySelector('#tt tbody'),rows=[].slice.call(tb.querySelectorAll('tr[data-status]')),filter='all',q='';
rows.forEach(function(r){var x=r.nextElementSibling;if(x&&x.classList.contains('xrow'))r.detail=x});
var cnt=document.getElementById('cnt');
function apply(){
  var n=0;
  rows.forEach(function(r){
    var ok=(filter==='all'||r.dataset.status===filter||(filter==='failed'&&r.dataset.status==='setup'))&&(!q||r.dataset.name.indexOf(q)>-1||r.dataset.mod.toLowerCase().indexOf(q)>-1);
    r.style.display=ok?'':'none'; if(ok)n++;
    if(r.detail)r.detail.style.display=ok&&r.classList.contains('open')?'':'none';
  });
  cnt.textContent=isEn()?('Showing '+n+' of '+rows.length+' tests'):('المعروض: '+n+' من '+rows.length);
}
document.querySelectorAll('.fc').forEach(function(b){
  b.addEventListener('click',function(){
    document.querySelectorAll('.fc').forEach(function(x){x.classList.remove('on')});
    b.classList.add('on');filter=b.dataset.f;apply();
  });
});
document.getElementById('q').addEventListener('input',function(e){q=e.target.value.trim().toLowerCase();apply()});
var sortKey='idx',dir=1;
document.querySelectorAll('#tt thead th').forEach(function(th){
  th.addEventListener('click',function(){
    var k=th.dataset.s; dir=(sortKey===k)?-dir:1; sortKey=k;
    document.querySelectorAll('#tt thead th').forEach(function(x){x.classList.remove('asc','desc')});
    th.classList.add(dir===1?'asc':'desc');
    var num=(k==='idx'||k==='dur');
    var key={idx:'idx',dur:'dur',name:'name',mod:'mod',status:'status'}[k];
    rows.sort(function(a,b){
      var x=a.dataset[key],y=b.dataset[key];
      if(num){x=+x;y=+y;return (x-y)*dir}
      return x.localeCompare(y)*dir;
    });
    rows.forEach(function(r){tb.appendChild(r);if(r.detail)tb.appendChild(r.detail)});
  });
});
function toggle(r){
  var open=!r.classList.contains('open');
  r.classList.toggle('open',open);r.setAttribute('aria-expanded',open);
  r.detail.style.display=open?'':'none';
  var l=r.detail.querySelector('.logs');if(open&&l)l.scrollTop=l.scrollHeight;
}
rows.forEach(function(r){
  if(!r.detail)return;
  r.addEventListener('click',function(e){if(!e.target.closest('a'))toggle(r)});
  r.addEventListener('keydown',function(e){if(e.key==='Enter'||e.key===' '){e.preventDefault();toggle(r)}});
});
document.querySelectorAll('a.shotlink').forEach(function(a){
  a.addEventListener('click',function(){
    if(!a.dataset.url){var b=atob(a.dataset.png),u=new Uint8Array(b.length);for(var i=0;i<b.length;i++)u[i]=b.charCodeAt(i);
      a.dataset.url=URL.createObjectURL(new Blob([u],{type:'image/png'}))}
    a.href=a.dataset.url;
  });
});
function setLang(l){
  root.lang=l;root.dir=l==='en'?'ltr':'rtl';
  document.title=l==='en'?'Test Result':'نتيجة الاختبار';
  document.querySelectorAll('[data-attr]').forEach(function(el){el.setAttribute(el.dataset.attr,el.dataset[l])});
  try{localStorage.setItem('result-report-lang',l)}catch(e){}
  apply();
}
document.getElementById('lang').addEventListener('click',function(){setLang(isEn()?'ar':'en')});
setLang(isEn()?'en':'ar');
})();
