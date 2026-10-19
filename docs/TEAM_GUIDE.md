# Company teams


1. Owner: open **More → Company team → New company** and enter the company and owner names.
2. Open **People → Team members → Invite member**. Enter the person's Google sign-in email and choose **office** or **worker**. Copy the invitation code and share it yourself. No email or message is sent automatically. Invitations expire after 7 days, are single-use and require the recipient's verified sign-in email.
3. Staff and workers install this APK and sign in through Google. On the personal storage-choice screen, choose **Join or create a company team**, then **Join company** and paste the code. They can use company records without creating a personal builder workspace. Existing users can enter through **More → Company team**.
4. Add shared projects in **Sites**. Owners with personal projects can use **Copy my project** to copy a selected project's name/year/location. Personal documents, buyer records, budgets and payments stay private. Use **Worker access** on a site to assign workers; office staff have company-wide site access.
5. Use **Work** to assign tasks and submit daily progress notes. Workers can change their own assigned task status.
6. Use **People/My pay → Attendance** to submit attendance. Office staff set a member's daily/hourly wage rate and approve attendance. **Wages → Record payment** records a payment already made. It does not send money. Wage totals use integer paise; approved earnings keep the approved rate. Rate, attendance and payment changes retain before/after audit rows.
7. In **Stock**, office staff add materials and record deliveries/issues. Workers request material for assigned sites; office staff issue or reject requests. The server refuses an issue above available stock and safely handles retries.

| Role | Company access |
|---|---|
| Owner | Company records, invitations, member roles/access, site assignments, wages, stock and reports |
| Office | Company projects, assigned work, member roster, attendance approval, wage records, materials and reports |
| Worker | Assigned projects, own tasks, own attendance/wages, assigned-site progress reports/stock and own material requests |

Role and project rules run in the database, including for direct API requests. Revoked members lose future company access. Shared company data is separate from personal account snapshots; no existing personal data is automatically published to a company. Owners retain ownership; ownership transfer and company deletion are not included.

Company screens need internet. Use Refresh for changes from other devices; this release does not include a realtime chat/notification service or an offline shared-write queue. Company CSV exports contain the records visible to the current role; attendance/payment lists are limited to the newest 1,000 entries and daily reports to 300, while wage balances use all approved attendance and recorded payments. A company CSV is a report, not a restorable company backup. Personal ZIP backup/restore does not include company data.

## Scope of this team release

Included: separate Google logins, companies, email-bound invitation codes, owner/office/worker roles, project assignments, tasks, daily progress text reports, manual attendance, daily/hourly wage calculations and payment records, stock movements/requests, CSV reporting and financial audit history.

Later modules: shared photo/document attachments, GPS/geofencing, facial recognition, monthly salary rules, chat/push/WhatsApp, equipment, BOQ, procurement orders/GRNs/RA billing, tax invoicing and Tally integrations. No placeholder controls claim these are active. The existing personal document vault and buyer/payment tools remain available in the personal workspace.
