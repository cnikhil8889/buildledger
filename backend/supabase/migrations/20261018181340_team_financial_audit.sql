-- Preserve the exact financial rows before and after corrections.
alter table public.bl_audit add column details jsonb not null default '{}'::jsonb;
create function buildledger_private.team_financial_audit() returns trigger
language plpgsql security definer set search_path='' as $$
declare previous jsonb; current_row jsonb:=to_jsonb(new); actor_id uuid:=auth.uid();
begin
 if actor_id is null then raise exception 'An authenticated actor is required for company financial changes.'; end if;
 if tg_op='UPDATE' then previous:=to_jsonb(old); if previous=current_row then return new; end if; end if;
 insert into public.bl_audit(company_id,actor,action,target,details)
 values(new.company_id,actor_id,case tg_table_name when 'bl_rates' then 'wage_rate_recorded' when 'bl_attendance' then 'attendance_recorded' else 'wage_payment_recorded' end,
 coalesce(current_row->>'id',current_row->>'user_id')::uuid,jsonb_build_object('before',previous,'after',current_row));
 return new;
end $$;
revoke all on function buildledger_private.team_financial_audit() from public,anon,authenticated;
create trigger bl_attendance_financial_audit after insert or update on public.bl_attendance for each row execute function buildledger_private.team_financial_audit();
create trigger bl_rates_financial_audit after insert or update on public.bl_rates for each row execute function buildledger_private.team_financial_audit();
create trigger bl_wages_financial_audit after insert or update on public.bl_wages for each row execute function buildledger_private.team_financial_audit();
