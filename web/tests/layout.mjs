import {chromium} from 'playwright-core';import {build} from 'esbuild';import fs from 'node:fs';import assert from 'node:assert/strict';import path from 'node:path';import {fileURLToPath,pathToFileURL} from 'node:url';
const here=path.dirname(fileURLToPath(import.meta.url));const output=path.join(here,'.build/layout');fs.mkdirSync(output,{recursive:true});
if(!process.env.CHROME_BIN)throw new Error('Set CHROME_BIN to a Chromium or Chrome headless-shell executable.');
await build({entryPoints:[path.join(here,'../src/lib/demo.ts')],bundle:true,platform:'node',format:'esm',outfile:path.join(output,'fixture.mjs')});const {demoWorkspace}=await import(pathToFileURL(path.join(output,'fixture.mjs')));
const browser=await chromium.launch({executablePath:process.env.CHROME_BIN,headless:true,args:['--no-sandbox']});
const results=[];const errors=[];
async function makePage(width,height,workspace=demoWorkspace,authenticated=true,storage='cloud'){
 const page=await browser.newPage({viewport:{width,height},deviceScaleFactor:1,isMobile:width<800,hasTouch:true});
 page.on('pageerror',e=>errors.push(e.message));
 await page.addInitScript(({workspace,authenticated,storage})=>{
  let current=workspace,revision=0;
  window.BuildLedgerNative={readAccount:()=>JSON.stringify({configured:true,authenticated,providers:['google'],storage,legacyAvailable:false,user:authenticated?{name:'Test Builder',email:'test@example.com'}:undefined,folder:'BuildLedger/private-test-folder'}),readWorkspace:()=>JSON.stringify({workspace:current,revision}),readFiles:()=>JSON.stringify({files:[]}),saveWorkspace:(raw,expected)=>{if(expected!==revision)return JSON.stringify({error:'Revision conflict'});current=JSON.parse(raw);revision++;return JSON.stringify({workspace:current,revision});},pickDocument(){},openDocument(){},syncCloud(){},downloadCloud(){},saveBackup(){},restoreBackup(){},signOut(){},showPrivacy(){},clearLocalData(){},exportCsv(){},printReport(){}};
 },{workspace,authenticated,storage});
 await page.goto(pathToFileURL(path.join(here,'../../app/src/main/assets/index.html')).href);
 return page;
}
async function fit(page,label){
 const state=await page.evaluate(()=>({width:document.documentElement.clientWidth,inner:innerWidth,scroll:document.documentElement.scrollWidth,body:document.body.scrollWidth,nav:[...document.querySelectorAll('.android-bottom-nav button')].map(e=>({text:e.innerText,...e.getBoundingClientRect().toJSON()})),over:[...document.querySelectorAll('main *')].filter(e=>!e.closest('.table-wrap')&& !e.closest('.project-art')).map(e=>({class:String(e.className),...e.getBoundingClientRect().toJSON()})).filter(e=>e.width>0&&(e.right>document.documentElement.clientWidth+1||e.left < -1)).slice(0,12)}));
 results.push({label,...state});fs.writeFileSync(path.join(output,'layout-results.json'),JSON.stringify(results,null,2));
 assert.equal(state.inner,page.viewportSize().width,label+' viewport expansion');assert.ok(state.scroll<=state.width+1,label+' root overflow');assert.ok(!state.over.length,label+' overflowing content '+JSON.stringify(state.over));
 if(page.viewportSize().width<=760){assert.equal(state.nav.length,5);for(const n of state.nav){assert.ok(n.x>=0&&n.right<=state.width+1,label+' clipped tab '+n.text);assert.ok(n.bottom<=page.viewportSize().height+1,label+' obscured nav')}}
}
for(const width of [320,360,393,412,600,760,800]){
 const page=await makePage(width,800);await page.getByRole('button',{name:'New project',exact:true}).waitFor();
 for(const [label,selector] of [['home',null],['projects','.android-bottom-nav >> text=Projects'],['files','.android-bottom-nav >> text=Files'],['ledger','.android-bottom-nav >> text=Ledger']]){
  if(selector&&width<=760)await page.locator(selector).click();else if(selector)await page.locator('.sidebar').getByRole('button',{name:{projects:/^Projects/,files:'Document vault',ledger:'Payments'}[label]}).click();
  await fit(page,width+' '+label);
 }
 if(width<=760)await page.locator('.android-bottom-nav').getByRole('button',{name:'More',exact:true}).click();await page.locator('.sidebar').getByRole('button',{name:'Buyers & agreements',exact:true}).click();await fit(page,width+' buyers');
 if(width<=760)await page.locator('.android-bottom-nav').getByRole('button',{name:'More',exact:true}).click();await page.locator('.sidebar').getByRole('button',{name:'Reports',exact:true}).click();await fit(page,width+' reports');
 if(width===393){await page.screenshot({path:path.join(output,'reports.png'),fullPage:true});await page.locator('.android-bottom-nav').getByRole('button',{name:'Home',exact:true}).click();await page.screenshot({path:path.join(output,'overview.png'),fullPage:true});}
 await page.close();
}
for(const scale of [1.3,1.5]){
 const large=await makePage(393,800);await large.getByRole('button',{name:'New project',exact:true}).waitFor();
 // Approximate Android WebView text zoom without changing fixed dp dimensions.
 await large.evaluate(scale=>{const sizes=[...document.querySelectorAll('body *')].filter(e=>e instanceof HTMLElement).map(e=>[e,parseFloat(getComputedStyle(e).fontSize)]);for(const [e,size] of sizes)e.style.fontSize=`${size*scale}px`;},scale);
 await fit(large,'text scale '+scale);await large.close();
}
const long=structuredClone(demoWorkspace);long.projects[0].name='A'.repeat(100);const longPage=await makePage(360,800,long);await longPage.getByRole('button',{name:'New project',exact:true}).waitFor();await fit(longPage,'long project name');await longPage.close();
const page=await makePage(393,800);await page.getByRole('button',{name:'New project',exact:true}).click();
const dialog=page.getByRole('dialog');await dialog.waitFor();assert.equal(await page.locator(':focus').getAttribute('aria-label'),'Close dialog');
await page.getByLabel('Project name',{exact:true}).fill('Screen fit test project');
await page.setViewportSize({width:393,height:390});await page.waitForFunction(()=>parseFloat(getComputedStyle(document.documentElement).getPropertyValue('--viewport-height'))<=390);
await page.getByLabel('Project notes',{exact:true}).fill('A test project for a small keyboard viewport.');
const modal=await page.evaluate(()=>{const box=s=>document.querySelector(s).getBoundingClientRect().toJSON();return {dialog:box('dialog'),head:box('.dialog-head'),save:box('.form-foot'),height:innerHeight,content:box('.dialog-content'),scroll:document.querySelector('.dialog-content').scrollHeight};});
results.push({label:'keyboard modal',...modal});fs.writeFileSync(path.join(output,'layout-results.json'),JSON.stringify(results,null,2));await page.screenshot({path:path.join(output,'keyboard-form.png')});console.log(modal);assert.ok(modal.head.top>=0&&modal.save.bottom<=modal.content.bottom+1&&modal.save.top>=modal.head.bottom,'dialog header and save must both fit');await page.screenshot({path:path.join(output,'keyboard-form.png')});
await page.getByRole('button',{name:'Create project & folders',exact:true}).click();await dialog.waitFor({state:'hidden'});await page.setViewportSize({width:393,height:800});await fit(page,'saved project');await page.close();
for(const [authenticated,storage,label] of [[false,'','login'],[true,'','storage']]){const page=await makePage(320,640,null,authenticated,storage);await page.locator('.onboarding-card').waitFor();const widths=await page.evaluate(()=>({width:innerWidth,scroll:document.documentElement.scrollWidth}));assert.equal(widths.width,320);assert.equal(widths.scroll,320);results.push({label,...widths});await page.screenshot({path:path.join(output,label+'.png'),fullPage:true});await page.close();}
assert.deepEqual(errors,[]);fs.writeFileSync(path.join(output,'layout-results.json'),JSON.stringify(results,null,2));await browser.close();console.log('Passed responsive views, keyboard form, project save and onboarding.');
