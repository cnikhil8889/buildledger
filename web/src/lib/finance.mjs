// Sum in paise to avoid floating-point drift.
export const sumMoney = (rows, field) => rows.reduce((sum,row)=>sum+Math.round(row[field]*100),0)/100;
export function totals(workspace, projectId) {
  const payments=workspace.payments.filter(p=>!projectId || p.projectId===projectId);
  const expense=payments.filter(p=>p.direction==='expense'), receipts=payments.filter(p=>p.direction==='receipt');
  const buyers=workspace.buyers.filter(b=>!projectId || b.projectId===projectId), projects=workspace.projects.filter(p=>!projectId || p.id===projectId);
  const committed=sumMoney(expense,'amount'), spent=sumMoney(expense,'paid'), collected=sumMoney(receipts,'paid'), sales=sumMoney(buyers,'saleValue'), budget=sumMoney(projects,'budget');
  return {budget,committed,spent,collected,sales,payable:Math.round((committed-spent)*100)/100,receivable:Math.round((sales-collected)*100)/100,remaining:Math.round((budget-spent)*100)/100,net:Math.round((collected-spent)*100)/100};
}
export function csvCell(value) {let s=String(value??'');if(/^[=+\-@\t\r]/.test(s))s="'"+s;return '"'+s.replace(/"/g,'""')+'"';}
