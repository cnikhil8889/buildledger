-- Company records are separate from existing personal account snapshots.
create table public.bl_companies(id uuid primary key default gen_random_uuid(),name text not null check(length(name) between 2 and 100),owner_id uuid not null references auth.users(id),created_at timestamptz not null default now());
create table public.bl_members(company_id uuid not null references public.bl_companies(id),user_id uuid not null references auth.users(id),email text not null,name text not null check(length(name) between 1 and 100),role text not null check(role in ('owner','office','worker')),active boolean not null default true,created_at timestamptz not null default now(),primary key(company_id,user_id));
create index bl_members_user on public.bl_members(user_id,active);
create table public.bl_invites(id uuid primary key default gen_random_uuid(),company_id uuid not null references public.bl_companies(id),email text not null,role text not null check(role in ('office','worker')),token_hash text not null unique,expires_at timestamptz not null default now()+interval '7 days',created_by uuid not null references auth.users(id),accepted_by uuid references auth.users(id),revoked boolean not null default false,created_at timestamptz not null default now());
create index bl_invites_company on public.bl_invites(company_id);
create table public.bl_projects(id uuid primary key default gen_random_uuid(),company_id uuid not null references public.bl_companies(id),name text not null check(length(name) between 2 and 100),year integer not null check(year between 1990 and 2100),location text not null default '' check(length(location)<=160),status text not null default 'Planning' check(status in ('Planning','In progress','Completed','On hold')),progress integer not null default 0 check(progress between 0 and 100),version integer not null default 1,created_at timestamptz not null default now(),unique(company_id,id));
create index bl_projects_company on public.bl_projects(company_id);
create table public.bl_assignments(company_id uuid not null,project_id uuid not null,user_id uuid not null,primary key(project_id,user_id),foreign key(company_id,project_id) references public.bl_projects(company_id,id),foreign key(company_id,user_id) references public.bl_members(company_id,user_id));
create index bl_assignments_member on public.bl_assignments(company_id,user_id);
create table public.bl_tasks(id uuid primary key default gen_random_uuid(),company_id uuid not null,project_id uuid not null,title text not null check(length(title) between 2 and 160),notes text not null default '' check(length(notes)<=2000),assignee uuid not null,due_date date,status text not null default 'Open' check(status in ('Open','In progress','Done')),version integer not null default 1,created_by uuid not null references auth.users(id),created_at timestamptz not null default now(),foreign key(company_id,project_id) references public.bl_projects(company_id,id),foreign key(company_id,assignee) references public.bl_members(company_id,user_id));
create index bl_tasks_project on public.bl_tasks(company_id,project_id);
create index bl_tasks_assignee on public.bl_tasks(company_id,assignee);
create table public.bl_reports(id uuid primary key default gen_random_uuid(),company_id uuid not null,project_id uuid not null,report_date date not null,notes text not null check(length(notes) between 2 and 4000),created_by uuid not null references auth.users(id),created_at timestamptz not null default now(),foreign key(company_id,project_id) references public.bl_projects(company_id,id));
create index bl_reports_project on public.bl_reports(company_id,project_id,report_date desc);
create table public.bl_rates(company_id uuid not null,user_id uuid not null,kind text not null check(kind in ('daily','hourly')),rate_paise bigint not null check(rate_paise between 0 and 100000000),primary key(company_id,user_id),foreign key(company_id,user_id) references public.bl_members(company_id,user_id));
create table public.bl_attendance(id uuid primary key default gen_random_uuid(),company_id uuid not null,project_id uuid not null,user_id uuid not null,work_date date not null,day_fraction numeric(3,2) not null check(day_fraction in (0,0.5,1)),hours numeric(4,2) not null check(hours between 0 and 24),status text not null default 'Pending' check(status in ('Pending','Approved','Voided')),earned_paise bigint not null default 0 check(earned_paise>=0),rate_paise bigint,rate_kind text,created_by uuid not null references auth.users(id),approved_by uuid references auth.users(id),created_at timestamptz not null default now(),unique(company_id,user_id,work_date),foreign key(company_id,project_id) references public.bl_projects(company_id,id),foreign key(company_id,user_id) references public.bl_members(company_id,user_id));
create index bl_attendance_project on public.bl_attendance(company_id,project_id);
create table public.bl_wages(id uuid primary key default gen_random_uuid(),company_id uuid not null,user_id uuid not null,amount_paise bigint not null check(amount_paise between 1 and 10000000000),paid_date date not null,reference text not null default '' check(length(reference)<=160),status text not null default 'Recorded' check(status in ('Recorded','Voided')),created_by uuid not null references auth.users(id),created_at timestamptz not null default now(),foreign key(company_id,user_id) references public.bl_members(company_id,user_id));
create index bl_wages_member on public.bl_wages(company_id,user_id);
create table public.bl_materials(id uuid primary key default gen_random_uuid(),company_id uuid not null references public.bl_companies(id),name text not null check(length(name) between 2 and 100),unit text not null check(length(unit) between 1 and 20),minimum numeric(16,3) not null default 0 check(minimum>=0),unique(company_id,id));
create index bl_materials_company on public.bl_materials(company_id);
create table public.bl_stock(id uuid primary key default gen_random_uuid(),company_id uuid not null,project_id uuid not null,material_id uuid not null,quantity numeric(16,3) not null check(quantity<>0),note text not null default '' check(length(note)<=300),created_by uuid not null references auth.users(id),created_at timestamptz not null default now(),foreign key(company_id,project_id) references public.bl_projects(company_id,id),foreign key(company_id,material_id) references public.bl_materials(company_id,id));
create index bl_stock_project_material on public.bl_stock(company_id,project_id,material_id);
create table public.bl_requests(id uuid primary key default gen_random_uuid(),company_id uuid not null,project_id uuid not null,material_id uuid not null,quantity numeric(16,3) not null check(quantity>0),note text not null default '' check(length(note)<=300),status text not null default 'Pending' check(status in ('Pending','Issued','Rejected')),created_by uuid not null references auth.users(id),created_at timestamptz not null default now(),foreign key(company_id,project_id) references public.bl_projects(company_id,id),foreign key(company_id,material_id) references public.bl_materials(company_id,id));
create index bl_requests_project on public.bl_requests(company_id,project_id);
create table public.bl_audit(id uuid primary key default gen_random_uuid(),company_id uuid not null references public.bl_companies(id),actor uuid not null references auth.users(id),action text not null,target uuid,created_at timestamptz not null default now());
create index bl_audit_company on public.bl_audit(company_id,created_at desc);
-- Receipts contain invitation secrets and are deliberately outside the Data API.
create table buildledger_private.team_receipts(actor uuid not null,request_id uuid not null,operation text not null,payload jsonb not null,result jsonb not null,primary key(actor,request_id));
alter table buildledger_private.team_receipts enable row level security;
revoke all on buildledger_private.team_receipts from public,anon,authenticated;

create function buildledger_private.team_role(c uuid) returns text language sql stable security definer set search_path='' as $$
 select m.role from public.bl_members m join auth.users u on u.id=m.user_id where m.company_id=c and m.user_id=auth.uid() and m.active and u.email_confirmed_at is not null and u.deleted_at is null
$$;
create function buildledger_private.team_project(p uuid) returns boolean language sql stable security definer set search_path='' as $$
 select exists(select 1 from public.bl_projects x where x.id=p and (buildledger_private.team_role(x.company_id) in ('owner','office') or (buildledger_private.team_role(x.company_id)='worker' and exists(select 1 from public.bl_assignments a where a.project_id=x.id and a.user_id=auth.uid()))))
$$;
revoke all on function buildledger_private.team_role(uuid),buildledger_private.team_project(uuid) from public,anon;
grant execute on function buildledger_private.team_role(uuid),buildledger_private.team_project(uuid) to authenticated;
-- Only SELECT is granted. All mutations go through the validated command RPC.
do $$ declare t text; begin
 foreach t in array array['bl_companies','bl_members','bl_invites','bl_projects','bl_assignments','bl_tasks','bl_reports','bl_rates','bl_attendance','bl_wages','bl_materials','bl_stock','bl_requests','bl_audit'] loop
 execute format('alter table public.%I enable row level security',t);
 execute format('revoke all on public.%I from public,anon,authenticated',t);
 execute format('grant select on public.%I to authenticated',t);
 end loop;
end $$;
create policy bl_company_read on public.bl_companies for select to authenticated using (buildledger_private.team_role(id) is not null);
create policy bl_members_read on public.bl_members for select to authenticated using (buildledger_private.team_role(company_id) in ('owner','office') or (user_id=(select auth.uid()) and buildledger_private.team_role(company_id) is not null));
create policy bl_invites_read on public.bl_invites for select to authenticated using (buildledger_private.team_role(company_id)='owner');
create policy bl_projects_read on public.bl_projects for select to authenticated using (buildledger_private.team_project(id));
create policy bl_assignments_read on public.bl_assignments for select to authenticated using (buildledger_private.team_role(company_id) in ('owner','office') or (user_id=(select auth.uid()) and buildledger_private.team_role(company_id) is not null));
create policy bl_tasks_read on public.bl_tasks for select to authenticated using (buildledger_private.team_project(project_id) and (buildledger_private.team_role(company_id) in ('owner','office') or assignee=(select auth.uid())));
create policy bl_reports_read on public.bl_reports for select to authenticated using (buildledger_private.team_project(project_id));
create policy bl_rates_read on public.bl_rates for select to authenticated using (buildledger_private.team_role(company_id) in ('owner','office') or (user_id=(select auth.uid()) and buildledger_private.team_role(company_id) is not null));
create policy bl_attendance_read on public.bl_attendance for select to authenticated using (buildledger_private.team_role(company_id) in ('owner','office') or (user_id=(select auth.uid()) and buildledger_private.team_role(company_id) is not null));
create policy bl_wages_read on public.bl_wages for select to authenticated using (buildledger_private.team_role(company_id) in ('owner','office') or (user_id=(select auth.uid()) and buildledger_private.team_role(company_id) is not null));
create policy bl_materials_read on public.bl_materials for select to authenticated using (buildledger_private.team_role(company_id) is not null);
create policy bl_stock_read on public.bl_stock for select to authenticated using (buildledger_private.team_project(project_id));
create policy bl_requests_read on public.bl_requests for select to authenticated using (buildledger_private.team_project(project_id) and (buildledger_private.team_role(company_id) in ('owner','office') or created_by=(select auth.uid())));
create policy bl_audit_read on public.bl_audit for select to authenticated using (buildledger_private.team_role(company_id) in ('owner','office'));

create function public.bl_team_read(company_id uuid default null) returns jsonb language plpgsql stable security invoker set search_path='' as $$
declare c uuid:=company_id; companies jsonb; result jsonb;
begin
 if auth.uid() is null then return jsonb_build_object('error','Sign in to open a company.'); end if;
 select coalesce(jsonb_agg(x),'[]'::jsonb) into companies from (select b.id,b.name,b.owner_id,m.role from public.bl_companies b join public.bl_members m on m.company_id=b.id and m.user_id=auth.uid() and m.active order by b.created_at limit 25) x;
 if c is null then c:=(companies->0->>'id')::uuid; end if;
 if c is null then return jsonb_build_object('companies',companies,'company',null); end if;
 if buildledger_private.team_role(c) is null then return jsonb_build_object('error','Company access has ended. Refresh your company list.'); end if;
 result:=jsonb_build_object('companies',companies,'company',c,'role',buildledger_private.team_role(c),'userId',auth.uid());
 result:=result||jsonb_build_object('members',(select coalesce(jsonb_agg(x),'[]') from (select * from public.bl_members m where m.company_id=c order by m.created_at limit 200) x));
 result:=result||jsonb_build_object('projects',(select coalesce(jsonb_agg(x),'[]') from (select * from public.bl_projects p where p.company_id=c order by p.created_at desc limit 200) x));
 result:=result||jsonb_build_object('assignments',(select coalesce(jsonb_agg(x),'[]') from (select * from public.bl_assignments a where a.company_id=c limit 4000) x));
 result:=result||jsonb_build_object('tasks',(select coalesce(jsonb_agg(x),'[]') from (select * from public.bl_tasks t where t.company_id=c order by t.created_at desc limit 1000) x));
 result:=result||jsonb_build_object('reports',(select coalesce(jsonb_agg(x),'[]') from (select * from public.bl_reports r where r.company_id=c order by r.report_date desc,r.created_at desc limit 300) x));
 result:=result||jsonb_build_object('rates',(select coalesce(jsonb_agg(x),'[]') from (select * from public.bl_rates r where r.company_id=c) x));
 result:=result||jsonb_build_object('attendance',(select coalesce(jsonb_agg(x),'[]') from (select * from public.bl_attendance a where a.company_id=c order by a.work_date desc,a.created_at desc limit 1000) x));
 result:=result||jsonb_build_object('wages',(select coalesce(jsonb_agg(x),'[]') from (select * from public.bl_wages w where w.company_id=c order by w.paid_date desc,w.created_at desc limit 1000) x));
 result:=result||jsonb_build_object('payroll',(select coalesce(jsonb_agg(x),'[]') from (select m.user_id,m.name,coalesce((select sum(a.earned_paise) from public.bl_attendance a where a.company_id=c and a.user_id=m.user_id and a.status='Approved'),0) as earned_paise,coalesce((select sum(w.amount_paise) from public.bl_wages w where w.company_id=c and w.user_id=m.user_id and w.status='Recorded'),0) as paid_paise from public.bl_members m where m.company_id=c and (buildledger_private.team_role(c) in ('owner','office') or m.user_id=auth.uid())) x));
 result:=result||jsonb_build_object('materials',(select coalesce(jsonb_agg(x),'[]') from (select * from public.bl_materials m where m.company_id=c order by m.name limit 500) x));
 result:=result||jsonb_build_object('stock',(select coalesce(jsonb_agg(x),'[]') from (select s.project_id,s.material_id,sum(s.quantity) as quantity from public.bl_stock s where s.company_id=c group by s.project_id,s.material_id) x));
 result:=result||jsonb_build_object('movements',(select coalesce(jsonb_agg(x),'[]') from (select * from public.bl_stock s where s.company_id=c order by s.created_at desc limit 300) x));
 result:=result||jsonb_build_object('requests',(select coalesce(jsonb_agg(x),'[]') from (select * from public.bl_requests r where r.company_id=c order by r.created_at desc limit 300) x));
 result:=result||jsonb_build_object('invites',(select coalesce(jsonb_agg(x),'[]') from (select i.id,i.email,i.role,i.expires_at,i.accepted_by,i.revoked from public.bl_invites i where i.company_id=c order by i.created_at desc limit 100) x));
 result:=result||jsonb_build_object('audit',(select coalesce(jsonb_agg(x),'[]') from (select * from public.bl_audit a where a.company_id=c order by a.created_at desc limit 100) x));
 return result;
end $$;
revoke all on function public.bl_team_read(uuid) from public,anon;
grant execute on function public.bl_team_read(uuid) to authenticated;

create function buildledger_private.team_action(operation text,payload jsonb,request_id uuid) returns jsonb language plpgsql security definer set search_path='' as $$
declare actor uuid:=auth.uid(); actor_email text; c uuid; p uuid; target uuid; member_id uuid; role_name text; role_target text; result jsonb; prior buildledger_private.team_receipts; invite public.bl_invites; attendance public.bl_attendance; rate public.bl_rates; req public.bl_requests; token text; qty numeric; balance numeric; amount bigint; n integer;
begin
 select lower(email) into actor_email from auth.users where id=actor and email_confirmed_at is not null and deleted_at is null;
 if actor_email is null then raise exception 'Sign in with a verified email first.'; end if;
 if request_id is null or payload is null or jsonb_typeof(payload)<>'object' or length(payload::text)>16000 then raise exception 'The request is invalid.'; end if;
 -- Serialize retries from the same account before consulting the receipt.
 perform pg_advisory_xact_lock(hashtextextended(actor::text,0));
 select * into prior from buildledger_private.team_receipts r where r.actor=auth.uid() and r.request_id=team_action.request_id;
 if found then
  if prior.operation<>operation or prior.payload<>payload then raise exception 'This request number was already used. Refresh and try again.'; end if;
  return prior.result;
 end if;
 if operation='create_company' then
  if (select count(*) from public.bl_members where user_id=actor and active)>=25 then raise exception 'This account has reached 25 company memberships.'; end if;
  if (select count(*) from public.bl_companies where owner_id=actor)>=5 then raise exception 'This account already owns five companies.'; end if;
  insert into public.bl_companies(name,owner_id) values(trim(payload->>'name'),actor) returning id into c;
  insert into public.bl_members(company_id,user_id,email,name,role) values(c,actor,actor_email,left(coalesce(nullif(trim(payload->>'ownerName'),''),'Owner'),100),'owner');
  target:=c;result:=jsonb_build_object('id',c);
 elsif operation='join_company' then
  token:=trim(payload->>'code');
  if length(token)<>72 then raise exception 'Paste the full invitation code.'; end if;
  select * into invite from public.bl_invites where token_hash=encode(sha256(convert_to(token,'UTF8')),'hex');
  if not found then raise exception 'This invitation is unavailable.'; end if;
  perform 1 from public.bl_companies where id=invite.company_id for update;
  select * into invite from public.bl_invites where token_hash=encode(sha256(convert_to(token,'UTF8')),'hex') for update;
  if not found or invite.revoked or invite.accepted_by is not null or invite.expires_at<=now() or invite.email<>actor_email then raise exception 'This invitation is expired, used, or belongs to another sign-in email.'; end if;
  c:=invite.company_id;
  if (select count(*) from public.bl_members where user_id=actor and active)>=25 then raise exception 'This account has reached 25 company memberships.'; end if;
  if buildledger_private.team_role(c) is not null then raise exception 'You already belong to this company.'; end if;
  if (select count(*) from public.bl_members where company_id=c and active)>=200 then raise exception 'This company has reached 200 active members.'; end if;
  insert into public.bl_members(company_id,user_id,email,name,role,active) values(c,actor,actor_email,trim(payload->>'name'),invite.role,true) on conflict(company_id,user_id) do update set role=excluded.role,name=excluded.name,active=true,email=excluded.email;
  update public.bl_invites set accepted_by=actor where id=invite.id;
  target:=actor;result:=jsonb_build_object('id',c);
 else
  c:=(payload->>'companyId')::uuid;
  perform 1 from public.bl_companies where id=c for update;
  role_name:=buildledger_private.team_role(c);
  if role_name is null then raise exception 'You do not have active access to this company.'; end if;
  if operation in ('invite','member_access','revoke_invite','void_wage','rename_company') and role_name<>'owner' then raise exception 'Only the company owner can do this.'; end if;
  if operation not in ('invite','member_access','revoke_invite','void_wage','rename_company','create_project','project_status','assign_project','create_task','task_status','create_report','set_rate','mark_attendance','approve_attendance','void_attendance','pay_wage','create_material','stock_movement','request_material','review_request') then raise exception 'Choose a supported company action.'; end if;
  if operation not in ('task_status','create_report','mark_attendance','request_material') and role_name='worker' then raise exception 'This action needs owner or office access.'; end if;
  if operation='rename_company' then
   update public.bl_companies set name=trim(payload->>'name') where id=c returning id into target;
  elsif operation='invite' then
   actor_email:=lower(trim(payload->>'email'));role_target:=payload->>'role';
   if actor_email !~ '^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$' or length(actor_email)>254 or role_target not in ('office','worker') then raise exception 'Enter a valid email and office or worker role.'; end if;
   if exists(select 1 from public.bl_members where company_id=c and email=actor_email and active) then raise exception 'This email already belongs to the company.'; end if;
   if (select count(*) from public.bl_invites where company_id=c and created_at>now()-interval '1 day')>=100 then raise exception 'The daily invitation limit has been reached.'; end if;
   token:=gen_random_uuid()::text||gen_random_uuid()::text;
   insert into public.bl_invites(company_id,email,role,token_hash,created_by) values(c,actor_email,role_target,encode(sha256(convert_to(token,'UTF8')),'hex'),actor) returning id into target;
   result:=jsonb_build_object('id',target,'code',token,'email',actor_email);
  elsif operation='revoke_invite' then
   update public.bl_invites set revoked=true where id=(payload->>'id')::uuid and company_id=c returning id into target;
  elsif operation='member_access' then
   member_id:=(payload->>'userId')::uuid;role_target:=payload->>'role';
   if member_id=actor or role_target not in ('office','worker') then raise exception 'The owner role cannot be changed here.'; end if;
   update public.bl_members set role=role_target,active=(payload->>'active')::boolean,name=coalesce(nullif(trim(payload->>'name'),''),name) where company_id=c and user_id=member_id and role<>'owner' returning user_id into target;
  elsif operation='create_project' then
   if (select count(*) from public.bl_projects where company_id=c)>=200 then raise exception 'The company has reached 200 projects.'; end if;
   insert into public.bl_projects(company_id,name,year,location) values(c,trim(payload->>'name'),(payload->>'year')::integer,trim(coalesce(payload->>'location',''))) returning id into target;
  elsif operation='project_status' then
   update public.bl_projects set name=coalesce(nullif(trim(payload->>'name'),''),name),location=coalesce(payload->>'location',location),year=coalesce((payload->>'year')::integer,year),status=payload->>'status',progress=(payload->>'progress')::integer,version=version+1 where company_id=c and id=(payload->>'id')::uuid and version=(payload->>'version')::integer returning id into target;
  elsif operation='assign_project' then
   p:=(payload->>'projectId')::uuid;member_id:=(payload->>'userId')::uuid;
   if not exists(select 1 from public.bl_projects where id=p and company_id=c) or not exists(select 1 from public.bl_members where company_id=c and user_id=member_id and active) then raise exception 'Choose an active company member and project.'; end if;
   if coalesce((payload->>'assigned')::boolean,true) then insert into public.bl_assignments values(c,p,member_id) on conflict do nothing;
   else delete from public.bl_assignments where company_id=c and project_id=p and user_id=member_id; end if;
   target:=p;
  elsif operation in ('create_task','create_report','mark_attendance','request_material','stock_movement') then
   p:=(payload->>'projectId')::uuid;
   if not exists(select 1 from public.bl_projects where id=p and company_id=c) or not buildledger_private.team_project(p) then raise exception 'This project is not assigned to you.'; end if;
   if operation='create_task' then
    if role_name='worker' then raise exception 'Only owner or office staff can assign tasks.'; end if;
    member_id:=(payload->>'assignee')::uuid;
    if not exists(select 1 from public.bl_members m where m.company_id=c and m.user_id=member_id and m.active and (m.role in ('owner','office') or exists(select 1 from public.bl_assignments a where a.project_id=p and a.user_id=member_id))) then raise exception 'Assign this worker to the project first.'; end if;
    insert into public.bl_tasks(company_id,project_id,title,notes,assignee,due_date,created_by) values(c,p,trim(payload->>'title'),trim(coalesce(payload->>'notes','')),member_id,nullif(payload->>'dueDate','')::date,actor) returning id into target;
   elsif operation='create_report' then
    if (payload->>'date')::date>(now() at time zone 'Asia/Kolkata')::date or (payload->>'date')::date<(now() at time zone 'Asia/Kolkata')::date-365 then raise exception 'Choose a report date within the last year.'; end if;
    insert into public.bl_reports(company_id,project_id,report_date,notes,created_by) values(c,p,(payload->>'date')::date,trim(payload->>'notes'),actor) returning id into target;
   elsif operation='mark_attendance' then
    if (payload->>'dayFraction')::numeric=0 and (payload->>'hours')::numeric<>0 then raise exception 'Absent attendance must have zero hours.'; end if;
    member_id:=case when role_name='worker' then actor else (payload->>'userId')::uuid end;
    if role_name='worker' and (payload->>'userId')::uuid<>actor then raise exception 'You can only submit your own attendance.'; end if;
    if not exists(select 1 from public.bl_members m where m.company_id=c and m.user_id=member_id and m.active and (m.role in ('owner','office') or exists(select 1 from public.bl_assignments a where a.project_id=p and a.user_id=member_id))) then raise exception 'Assign this worker to the project first.'; end if;
    if (payload->>'date')::date>(now() at time zone 'Asia/Kolkata')::date or (payload->>'date')::date<(now() at time zone 'Asia/Kolkata')::date-90 then raise exception 'Choose an attendance date within the last 90 days.'; end if;
    select * into attendance from public.bl_attendance where company_id=c and user_id=member_id and work_date=(payload->>'date')::date for update;
    if found and attendance.status='Approved' then raise exception 'Approved attendance must be voided by office staff before correcting it.'; end if;
    insert into public.bl_attendance(company_id,project_id,user_id,work_date,day_fraction,hours,created_by) values(c,p,member_id,(payload->>'date')::date,(payload->>'dayFraction')::numeric,(payload->>'hours')::numeric,actor) on conflict(company_id,user_id,work_date) do update set project_id=excluded.project_id,day_fraction=excluded.day_fraction,hours=excluded.hours,status='Pending',earned_paise=0,rate_paise=null,rate_kind=null,approved_by=null returning id into target;
   elsif operation='request_material' then
    insert into public.bl_requests(company_id,project_id,material_id,quantity,note,created_by) values(c,p,(payload->>'materialId')::uuid,(payload->>'quantity')::numeric,trim(coalesce(payload->>'note','')),actor) returning id into target;
   else
    qty:=(payload->>'quantity')::numeric;
    if qty is null or qty<=0 or payload->>'direction' not in ('receipt','issue') then raise exception 'Enter a positive quantity and stock direction.'; end if;
    perform 1 from public.bl_materials where id=(payload->>'materialId')::uuid and company_id=c for update;
    if not found then raise exception 'Choose a company material.'; end if;
    select coalesce(sum(quantity),0) into balance from public.bl_stock where company_id=c and project_id=p and material_id=(payload->>'materialId')::uuid;
    if payload->>'direction'='issue' then qty:=-qty; end if;
    if balance+qty<0 then raise exception 'There is not enough stock at this project.'; end if;
    insert into public.bl_stock(company_id,project_id,material_id,quantity,note,created_by) values(c,p,(payload->>'materialId')::uuid,qty,trim(coalesce(payload->>'note','')),actor) returning id into target;
   end if;
  elsif operation='task_status' then
   update public.bl_tasks set status=payload->>'status',version=version+1 where company_id=c and id=(payload->>'id')::uuid and version=(payload->>'version')::integer and buildledger_private.team_project(project_id) and (role_name in ('owner','office') or assignee=actor) returning id into target;
  elsif operation='set_rate' then
   member_id:=(payload->>'userId')::uuid;
   insert into public.bl_rates(company_id,user_id,kind,rate_paise) values(c,member_id,payload->>'kind',(payload->>'ratePaise')::bigint) on conflict(company_id,user_id) do update set kind=excluded.kind,rate_paise=excluded.rate_paise returning user_id into target;
  elsif operation='approve_attendance' then
   select * into attendance from public.bl_attendance where company_id=c and id=(payload->>'id')::uuid for update;
   if not found or attendance.status<>'Pending' then raise exception 'This attendance is no longer waiting for approval.'; end if;
   select * into rate from public.bl_rates where company_id=c and user_id=attendance.user_id;
   if not found then raise exception 'Set a wage rate for this member before approving attendance.'; end if;
   amount:=round(rate.rate_paise*case when rate.kind='daily' then attendance.day_fraction else attendance.hours end)::bigint;
   update public.bl_attendance set status='Approved',earned_paise=amount,rate_paise=rate.rate_paise,rate_kind=rate.kind,approved_by=actor where id=attendance.id returning id into target;
  elsif operation='void_attendance' then
   update public.bl_attendance set status='Voided',earned_paise=0 where company_id=c and id=(payload->>'id')::uuid and status<>'Voided' returning id into target;
  elsif operation='pay_wage' then
   insert into public.bl_wages(company_id,user_id,amount_paise,paid_date,reference,created_by) values(c,(payload->>'userId')::uuid,(payload->>'amountPaise')::bigint,(payload->>'date')::date,trim(coalesce(payload->>'reference','')),actor) returning id into target;
  elsif operation='void_wage' then
   update public.bl_wages set status='Voided' where company_id=c and id=(payload->>'id')::uuid and status='Recorded' returning id into target;
  elsif operation='create_material' then
   if (select count(*) from public.bl_materials where company_id=c)>=500 then raise exception 'The company has reached 500 materials.'; end if;
   insert into public.bl_materials(company_id,name,unit,minimum) values(c,trim(payload->>'name'),trim(payload->>'unit'),(payload->>'minimum')::numeric) returning id into target;
  elsif operation='review_request' then
   select * into req from public.bl_requests where company_id=c and id=(payload->>'id')::uuid for update;
   if not found or req.status<>'Pending' then raise exception 'This material request has already been reviewed.'; end if;
   if payload->>'decision'='issue' then
    perform 1 from public.bl_materials where id=req.material_id for update;
    select coalesce(sum(quantity),0) into balance from public.bl_stock where company_id=c and project_id=req.project_id and material_id=req.material_id;
    if balance<req.quantity then raise exception 'There is not enough stock to issue this request.'; end if;
    insert into public.bl_stock(company_id,project_id,material_id,quantity,note,created_by) values(c,req.project_id,req.material_id,-req.quantity,'Issued material request',actor);
    update public.bl_requests set status='Issued' where id=req.id;
   elsif payload->>'decision'='reject' then update public.bl_requests set status='Rejected' where id=req.id;
   else raise exception 'Choose Issue or Reject.'; end if;
   target:=req.id;
  end if;
  if target is null then raise exception 'This record changed or is not available. Refresh and try again.'; end if;
 end if;
 insert into public.bl_audit(company_id,actor,action,target) values(c,actor,operation,target);
 result:=coalesce(result,jsonb_build_object('id',target));
 insert into buildledger_private.team_receipts values(actor,request_id,operation,payload,result);
 return result;
exception
 when raise_exception then return jsonb_build_object('error',sqlerrm);
 when invalid_text_representation or not_null_violation or check_violation or numeric_value_out_of_range or datetime_field_overflow then return jsonb_build_object('error','Check the form values and try again.');
 when foreign_key_violation then return jsonb_build_object('error','Choose records from this company and an active member.');
 when unique_violation then return jsonb_build_object('error','This record already exists. Refresh before trying again.');
end $$;
revoke all on function buildledger_private.team_action(text,jsonb,uuid) from public,anon;
grant execute on function buildledger_private.team_action(text,jsonb,uuid) to authenticated;
create function public.bl_team_action(operation text,payload jsonb,request_id uuid) returns jsonb language sql security invoker set search_path='' as $$select buildledger_private.team_action(operation,payload,request_id)$$;
revoke all on function public.bl_team_action(text,jsonb,uuid) from public,anon;
grant execute on function public.bl_team_action(text,jsonb,uuid) to authenticated;
notify pgrst,'reload schema';
