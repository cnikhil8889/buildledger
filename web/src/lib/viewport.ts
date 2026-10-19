/** The native shell already consumes system and keyboard insets. Track its
 * remaining viewport so dialogs also fit older Android WebView versions. */
export function watchViewport(){
 const root=document.documentElement;
 let frame=0;
 const update=()=>{
  cancelAnimationFrame(frame);
  const viewport=window.visualViewport;
   // Pinch zoom must remain available without relaying out the application.
   const height=viewport&&viewport.scale===1?Math.min(innerHeight,viewport.height):innerHeight;
   root.style.setProperty('--viewport-height',`${Math.round(height)}px`);
   const nav=document.querySelector('.android-bottom-nav,.team-bottom-nav');
   if(nav)root.style.setProperty('--bottom-nav-height',`${nav.getBoundingClientRect().height}px`);
  frame=requestAnimationFrame(()=>{
   const active=document.activeElement;
   if(active instanceof HTMLElement&&active.closest('dialog')&&active.matches('input,textarea,select'))active.scrollIntoView({block:'nearest'});
  });
 };
 window.addEventListener('resize',update);
 window.visualViewport?.addEventListener('resize',update);
 const observer=new ResizeObserver(update);
 // Observe the navigation once React has mounted it, including text scaling.
 const mounted=new MutationObserver(()=>{const nav=document.querySelector('.android-bottom-nav,.team-bottom-nav');if(nav){observer.observe(nav);mounted.disconnect();update();}});
 mounted.observe(document.getElementById('root')!,{childList:true,subtree:true});
 update();
}
