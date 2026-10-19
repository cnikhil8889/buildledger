-- Synthetic actors exist only inside this transaction. No real user records are touched.
begin;
insert into auth.users(id,email,email_confirmed_at) values
('00000000-0000-4000-9000-000000000301','bl-team-owner@invalid.example',now()),
('00000000-0000-4000-9000-000000000302','bl-team-worker@invalid.example',now()),
('00000000-0000-4000-9000-000000000303','bl-team-other@invalid.example',now());
create temporary table bl_verify(k text primary key,v jsonb);
grant select,insert,update on bl_verify to authenticated;
set local role authenticated;
select set_config('request.jwt.claim.sub','00000000-0000-4000-9000-000000000301',true);
insert into bl_verify values('company',public.bl_team_action('create_company','{"name":"Rollback Verification Company","ownerName":"Owner"}',gen_random_uuid()));
insert into bl_verify values('invite',public.bl_team_action('invite',jsonb_build_object('companyId',(select v->>'id' from bl_verify where k='company'),'email','bl-team-worker@invalid.example','role','worker'),gen_random_uuid()));
insert into bl_verify values('project',public.bl_team_action('create_project',jsonb_build_object('companyId',(select v->>'id' from bl_verify where k='company'),'name','Verification Site','year',2026,'location','Test'),gen_random_uuid()));
do $$ begin if exists(select 1 from bl_verify where v?'error') then raise exception 'Company setup failed';end if;end $$;
select set_config('request.jwt.claim.sub','00000000-0000-4000-9000-000000000302',true);
insert into bl_verify values('joined',public.bl_team_action('join_company',jsonb_build_object('code',(select v->>'code' from bl_verify where k='invite'),'name','Worker'),gen_random_uuid()));
do $$ begin if (select count(*) from public.bl_projects)<>0 then raise exception 'Unassigned project leaked';end if;end $$;
select set_config('request.jwt.claim.sub','00000000-0000-4000-9000-000000000301',true);
insert into bl_verify values('assigned',public.bl_team_action('assign_project',jsonb_build_object('companyId',(select v->>'id' from bl_verify where k='company'),'projectId',(select v->>'id' from bl_verify where k='project'),'userId','00000000-0000-4000-9000-000000000302','assigned',true),gen_random_uuid()));
insert into bl_verify values('rate',public.bl_team_action('set_rate',jsonb_build_object('companyId',(select v->>'id' from bl_verify where k='company'),'userId','00000000-0000-4000-9000-000000000302','kind','daily','ratePaise',75000),gen_random_uuid()));
select set_config('request.jwt.claim.sub','00000000-0000-4000-9000-000000000302',true);
do $$ begin if (select count(*) from public.bl_projects)<>1 then raise exception 'Assigned project unavailable';end if;end $$;
insert into bl_verify values('attendance',public.bl_team_action('mark_attendance',jsonb_build_object('companyId',(select v->>'id' from bl_verify where k='company'),'projectId',(select v->>'id' from bl_verify where k='project'),'userId','00000000-0000-4000-9000-000000000302','date',(now() at time zone 'Asia/Kolkata')::date,'dayFraction',1,'hours',8),gen_random_uuid()));
do $$ declare r jsonb;begin r:=public.bl_team_action('approve_attendance',jsonb_build_object('companyId',(select v->>'id' from bl_verify where k='company'),'id',(select v->>'id' from bl_verify where k='attendance')),gen_random_uuid());if not(r?'error') then raise exception 'Worker approved wages';end if;if exists(select 1 from public.bl_attendance where earned_paise<>0) then raise exception 'Unapproved wages calculated';end if;end $$;
select set_config('request.jwt.claim.sub','00000000-0000-4000-9000-000000000301',true);
insert into bl_verify values('approved',public.bl_team_action('approve_attendance',jsonb_build_object('companyId',(select v->>'id' from bl_verify where k='company'),'id',(select v->>'id' from bl_verify where k='attendance')),gen_random_uuid()));
insert into bl_verify values('payment',public.bl_team_action('pay_wage',jsonb_build_object('companyId',(select v->>'id' from bl_verify where k='company'),'userId','00000000-0000-4000-9000-000000000302','amountPaise',25000,'date',(now() at time zone 'Asia/Kolkata')::date,'reference','Rollback test'),'00000000-0000-4000-9000-000000000399'));
select public.bl_team_action('pay_wage',jsonb_build_object('companyId',(select v->>'id' from bl_verify where k='company'),'userId','00000000-0000-4000-9000-000000000302','amountPaise',25000,'date',(now() at time zone 'Asia/Kolkata')::date,'reference','Rollback test'),'00000000-0000-4000-9000-000000000399');
do $$ begin if exists(select 1 from bl_verify where v?'error') or (select count(*) from public.bl_wages)<>1 or (select earned_paise from public.bl_attendance limit 1)<>75000 then raise exception 'Payroll or retry verification failed';end if;end $$;
select set_config('request.jwt.claim.sub','00000000-0000-4000-9000-000000000303',true);
do $$ begin if (select count(*) from public.bl_companies)<>0 or (select count(*) from public.bl_wages)<>0 then raise exception 'Cross-company data leaked';end if;end $$;
select set_config('request.jwt.claim.sub','00000000-0000-4000-9000-000000000301',true);
select public.bl_team_action('member_access',jsonb_build_object('companyId',(select v->>'id' from bl_verify where k='company'),'userId','00000000-0000-4000-9000-000000000302','role','worker','active',false),gen_random_uuid());
select set_config('request.jwt.claim.sub','00000000-0000-4000-9000-000000000302',true);
do $$ begin if (select count(*) from public.bl_projects)<>0 or (select count(*) from public.bl_wages)<>0 then raise exception 'Revoked member retained access';end if;end $$;
reset role;
rollback;
select jsonb_build_object('synthetic_users_remaining',(select count(*) from auth.users where id in ('00000000-0000-4000-9000-000000000301','00000000-0000-4000-9000-000000000302','00000000-0000-4000-9000-000000000303')),'synthetic_companies_remaining',(select count(*) from public.bl_companies where owner_id='00000000-0000-4000-9000-000000000301')) as verification;
