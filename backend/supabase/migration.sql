create schema if not exists buildledger_private;
revoke all on schema buildledger_private from public, anon;
grant usage on schema buildledger_private to authenticated;

-- Apply once to the owner's Supabase project. No public project data access.
create table if not exists public.buildledger_snapshots (
  user_id uuid primary key references auth.users(id) on delete cascade,
  revision bigint not null default 0,
  object_path text,
  sha256 text,
  byte_size bigint,
  updated_at timestamptz not null default now()
);
alter table public.buildledger_snapshots enable row level security;
revoke all on public.buildledger_snapshots from anon, authenticated;
grant select on public.buildledger_snapshots to authenticated;
create policy "Read own BuildLedger snapshot" on public.buildledger_snapshots
  for select to authenticated using ((select auth.uid()) = user_id);

create or replace function buildledger_private.commit_snapshot(expected_revision bigint, new_path text, new_sha256 text, new_size bigint)
returns bigint language plpgsql security definer set search_path = '' as $$
declare owner_id uuid := auth.uid(); next_revision bigint;
begin
  if owner_id is null then raise exception 'Authentication required' using errcode = '42501'; end if;
  if expected_revision is null or expected_revision < 0 or new_path is null or new_sha256 is null or new_size is null
     or new_path !~ ('^' || owner_id::text || '/BuildLedger/snapshots/[a-f0-9-]{36}\.zip$')
     or new_sha256 !~ '^[a-f0-9]{64}$' or new_size <= 0 or new_size > 268144000 then
    raise exception 'Invalid snapshot';
  end if;
  if not exists (select 1 from storage.objects where bucket_id = 'buildledger' and name = new_path) then
    raise exception 'Upload the snapshot first';
  end if;
  insert into public.buildledger_snapshots(user_id) values (owner_id) on conflict do nothing;
  update public.buildledger_snapshots set revision = revision + 1, object_path = new_path,
    sha256 = new_sha256, byte_size = new_size, updated_at = now()
    where user_id = owner_id and revision = expected_revision returning revision into next_revision;
  if not found then raise exception 'Cloud revision conflict' using errcode = 'PT409'; end if;
  return next_revision;
end $$;
revoke all on function buildledger_private.commit_snapshot(bigint,text,text,bigint) from public, anon;
grant execute on function buildledger_private.commit_snapshot(bigint,text,text,bigint) to authenticated;


-- The public RPC is an invoker wrapper; privileged code lives outside exposed schemas.
create or replace function public.bl_commit_snapshot(expected_revision bigint, new_path text, new_sha256 text, new_size bigint)
returns bigint language sql security invoker set search_path = '' as $$
  select buildledger_private.commit_snapshot(expected_revision,new_path,new_sha256,new_size)
$$;
revoke all on function public.bl_commit_snapshot(bigint,text,text,bigint) from public, anon;
grant execute on function public.bl_commit_snapshot(bigint,text,text,bigint) to authenticated;

insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types)
values ('buildledger','buildledger',false,268144000,array['application/zip','application/json'])
on conflict (id) do update set public = false;
-- A JWT can outlive account deletion. Deleted accounts must also lose object access.
create or replace function buildledger_private.active_user() returns boolean
language sql stable security definer set search_path = '' as $$
  select exists (select 1 from auth.users where id = auth.uid())
$$;
revoke all on function buildledger_private.active_user() from public, anon;
grant execute on function buildledger_private.active_user() to authenticated;
create policy "BuildLedger own folder read" on storage.objects for select to authenticated
  using (bucket_id = 'buildledger' and (select buildledger_private.active_user()) and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy "BuildLedger own folder upload" on storage.objects for insert to authenticated
  with check (bucket_id = 'buildledger' and (select buildledger_private.active_user()) and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy "BuildLedger own folder update" on storage.objects for update to authenticated
  using (bucket_id = 'buildledger' and (select buildledger_private.active_user()) and (storage.foldername(name))[1] = (select auth.uid())::text)
  with check (bucket_id = 'buildledger' and (select buildledger_private.active_user()) and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy "BuildLedger own folder delete" on storage.objects for delete to authenticated
  using (bucket_id = 'buildledger' and (select buildledger_private.active_user()) and (storage.foldername(name))[1] = (select auth.uid())::text);
