import test from 'node:test';
import assert from 'node:assert/strict';
import {mkdir} from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath,pathToFileURL} from 'node:url';
import {build} from 'esbuild';
import React from 'react';
import TestRenderer,{act} from 'react-test-renderer';
const here=path.dirname(fileURLToPath(import.meta.url));await mkdir(here+'/.build',{recursive:true});
await build({entryPoints:[here+'/../src/AccountApp.tsx'],bundle:true,platform:'node',format:'esm',packages:'external',tsconfig:here+'/../tsconfig.json',outfile:here+'/.build/onboarding.mjs'});
const {default:AccountApp}=await import(pathToFileURL(here+'/.build/onboarding.mjs'));
const text=n=>typeof n==='string'?n:n?.props?.['aria-hidden']?'':(n?.children||[]).map(text).join('');
async function scenario(initial,run){
 const prior=globalThis.window;globalThis.IS_REACT_ACT_ENVIRONMENT=true;
 let status=initial,calls=[],renderer;const events=new Map();
 globalThis.window={addEventListener:(name,f)=>events.set(name,f),removeEventListener:name=>events.delete(name),BuildLedgerNative:{readAccount:()=>JSON.stringify(status),signIn:provider=>calls.push(['signIn',provider]),chooseStorage:mode=>calls.push(['storage',mode]),useExistingPhone:()=>calls.push(['legacy']),showPrivacy(){},signOut(){},readWorkspace:()=>JSON.stringify({workspace:null,revision:0}),readFiles:()=>JSON.stringify({files:[]})}};
 const button=label=>{const b=renderer.root.findAllByType('button').find(b=>text(b).trim()===label);assert.ok(b,label);return b;};
 try{await act(async()=>{renderer=TestRenderer.create(React.createElement(AccountApp),{createNodeMock:e=>e.type==='dialog'?{showModal(){}}:null});});await run({button,calls,renderer:()=>renderer,setStatus:async next=>{status=next;await act(async()=>events.get('buildledger-account-changed')());},click:async label=>act(async()=>button(label).props.onClick())});}
 finally{if(renderer)await act(async()=>renderer.unmount());globalThis.window=prior;delete globalThis.IS_REACT_ACT_ENVIRONMENT;}
}
test('Unconfigured sign-in stays honest and preserves access to earlier phone records',()=>scenario({configured:false,authenticated:false,legacyAvailable:true},async({button,calls,click})=>{
 assert.equal(button('Continue with Google').props.disabled,true);assert.equal(button('Continue with Apple').props.disabled,true);assert.equal(button('Continue with Microsoft').props.disabled,true);
 await click('Open my earlier phone workspace');assert.deepEqual(calls,[['legacy']]);
}));
test('Google login precedes storage; cloud failure does not open the workspace',()=>scenario({configured:true,authenticated:false,legacyAvailable:false},async({button,calls,click,setStatus,renderer})=>{
 await click('Continue with Google');assert.deepEqual(calls,[['signIn','google']]);
 await setStatus({configured:true,authenticated:true,legacyAvailable:false,user:{id:'abc',name:'Builder',email:'builder@example.com'},storage:''});
 const cloud=renderer().root.findAllByType('button').find(b=>b.props.role==='radio'&&text(b).includes('BuildLedger Cloud'));await act(async()=>cloud.props.onClick());await click('Continue with cloud storage');assert.deepEqual(calls.at(-1),['storage','cloud']);
 await setStatus({configured:true,authenticated:true,legacyAvailable:false,user:{id:'abc',name:'Builder',email:'builder@example.com'},storage:'',error:'Cloud is offline. Phone records were kept.'});
 assert.ok(renderer().root.findAllByProps({role:'alert'}).length);assert.ok(button('Continue with cloud storage'));
}));
test('Microsoft and Apple invoke their real provider identifiers',async()=>{
 for(const [label,id] of [['Apple','apple'],['Microsoft','azure']])await scenario({configured:true,authenticated:false,legacyAvailable:false},async({click,calls})=>{await click('Continue with '+label);assert.deepEqual(calls,[['signIn',id]]);});
});
test('Phone choice stays in onboarding until native storage confirms success',()=>scenario({configured:true,authenticated:true,legacyAvailable:false,user:{id:'a',name:'Builder',email:'builder@example.com'},storage:''},async({click,calls,button})=>{
 await click('Continue with phone storage');assert.deepEqual(calls,[['storage','mobile']]);assert.equal(button('Preparing your workspace…').props.disabled,true);
}));

test('Only enabled identity providers can be selected in the connected app',()=>scenario({configured:true,authenticated:false,legacyAvailable:false,providers:['google']},async({button,renderer})=>{
 assert.equal(button('Continue with Google').props.disabled,false);
 for(const provider of ['apple','azure']){const option=renderer().root.findAllByType('button').find(b=>b.props.className==='provider-button '+provider);assert.equal(option.props.disabled,true);assert.ok(text(option).includes('Setup pending'));}
}));
