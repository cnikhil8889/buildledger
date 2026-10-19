import {createRequire} from 'node:module';
import {readFile,writeFile,mkdir,copyFile} from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.dirname(fileURLToPath(import.meta.url));const require=createRequire(import.meta.url);
let esbuild;try{esbuild=require('esbuild');}catch{esbuild=createRequire(require.resolve('wrangler'))('esbuild');}
const tailwind=require('@tailwindcss/postcss');let postcss;try{postcss=require('postcss');}catch{postcss=createRequire(require.resolve('@tailwindcss/postcss'))('postcss');}
const output=path.resolve(root,'../app/src/main/assets');await mkdir(output,{recursive:true});
await esbuild.build({entryPoints:[root+'/src/index.tsx'],bundle:true,format:'iife',platform:'browser',target:['chrome80'],minify:true,sourcemap:false,define:{'process.env.NODE_ENV':'"production"'},tsconfig:root+'/tsconfig.json',outfile:output+'/app.js'});
const source=await readFile(root+'/src/app.css','utf8')+'\n'+await readFile(root+'/src/android.css','utf8')+'\n'+await readFile(root+'/src/team.css','utf8');const css=await postcss([tailwind({base:root})]).process(source,{from:root+'/src/app.css',to:output+'/app.css'});await writeFile(output+'/app.css',css.css);
await writeFile(output+'/index.html','<!doctype html><html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover"><meta name="theme-color" content="#1b2c43"><title>BuildLedger</title><link rel="icon" href="./favicon.svg"><link rel="stylesheet" href="./app.css"><script src="./app.js" defer></script></head><body><div id="root"></div></body></html>');
console.log('Packaged offline UI: app.js, app.css, index.html');

for(const file of ['brand-logo.png','brand-wordmark.png'])await copyFile(root+'/public/'+file,output+'/'+file);
