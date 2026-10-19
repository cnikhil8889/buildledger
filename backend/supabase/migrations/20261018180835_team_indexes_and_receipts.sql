-- Explicit deny policy for private idempotency receipts; the command function owns its writes.
create policy bl_receipts_no_client_access on buildledger_private.team_receipts for all to authenticated using (false) with check (false);
create index bl_assignments_company_id_project_id_fk on public.bl_assignments(company_id,project_id);
create index bl_attendance_approved_by_fk on public.bl_attendance(approved_by);
create index bl_attendance_created_by_fk on public.bl_attendance(created_by);
create index bl_audit_actor_fk on public.bl_audit(actor);
create index bl_companies_owner_id_fk on public.bl_companies(owner_id);
create index bl_invites_accepted_by_fk on public.bl_invites(accepted_by);
create index bl_invites_created_by_fk on public.bl_invites(created_by);
create index bl_reports_created_by_fk on public.bl_reports(created_by);
create index bl_requests_company_id_material_id_fk on public.bl_requests(company_id,material_id);
create index bl_requests_created_by_fk on public.bl_requests(created_by);
create index bl_stock_company_id_material_id_fk on public.bl_stock(company_id,material_id);
create index bl_stock_created_by_fk on public.bl_stock(created_by);
create index bl_tasks_created_by_fk on public.bl_tasks(created_by);
create index bl_wages_created_by_fk on public.bl_wages(created_by);
