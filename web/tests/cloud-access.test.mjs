import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import {PGlite} from '@electric-sql/pglite';

test('Cloud migration enforces account isolation and refuses stale financial snapshots',async()=>{
 const db=new PGlite();
 const alice='678a81bc-a934-42a3-9867-d9b4ae61a1f4',bob='abcdaaaa-1234-4567-8910-abcdefabcdef';
 const path=alice+'/BuildLedger/snapshots/99999999-1234-4567-8910-abcdefabcdef.zip',sha='a'.repeat(64);
 try{
  await db.exec(`create role anon;create role authenticated;
   create schema auth;create schema storage;
   create table auth.users(id uuid primary key);
   create function auth.uid() returns uuid language sql stable as $$select nullif(current_setting('request.jwt.claim.sub',true),'')::uuid$$;
   create table storage.buckets(id text primary key,name text,public boolean,file_size_limit bigint,allowed_mime_types text[]);
   create table storage.objects(id uuid default gen_random_uuid(),bucket_id text,name text);
   alter table storage.objects enable row level security;
   create function storage.foldername(name text) returns text[] language sql immutable as $$select string_to_array(name,'/')$$;
   grant usage on schema public,auth,storage to authenticated,anon;
   grant select,insert,update,delete on storage.objects to authenticated;
   insert into auth.users values ('${alice}'),('${bob}');`);
  await db.exec(await readFile(new URL('../../backend/supabase/migration.sql',import.meta.url),'utf8'));
  await db.exec(`set role authenticated;set request.jwt.claim.sub='${alice}';`);
  await db.query('insert into storage.objects(bucket_id,name) values ($1,$2)',['buildledger',path]);
  const commit=expected=>db.query('select public.bl_commit_snapshot($1,$2,$3,$4) as revision',[expected,path,sha,100]);
  assert.equal((await commit(0)).rows[0].revision,1);
  await assert.rejects(commit(0),e=>e.code==='PT409');
  assert.equal((await db.query('select revision from public.buildledger_snapshots')).rows[0].revision,1);
  await assert.rejects(db.query("update public.buildledger_snapshots set revision=50"),e=>e.code==='42501');
  await db.exec(`set request.jwt.claim.sub='${bob}';`);
  assert.equal((await db.query('select * from public.buildledger_snapshots')).rows.length,0);
  assert.equal((await db.query('select * from storage.objects')).rows.length,0);
  await assert.rejects(db.query('insert into storage.objects(bucket_id,name) values ($1,$2)',['buildledger',alice+'/BuildLedger/.folder.json']),e=>e.code==='42501');
  await assert.rejects(commit(0));
  await db.exec('set role anon;');
  await assert.rejects(db.query('select * from public.buildledger_snapshots'),e=>e.code==='42501');
  await db.exec(`reset role;delete from auth.users where id='${alice}';set role authenticated;set request.jwt.claim.sub='${alice}';`);
  assert.equal((await db.query('select * from storage.objects')).rows.length,0);
  await assert.rejects(db.query('insert into storage.objects(bucket_id,name) values ($1,$2)',['buildledger',alice+'/BuildLedger/.folder.json']),e=>e.code==='42501');
 }finally{await db.close();}
});
