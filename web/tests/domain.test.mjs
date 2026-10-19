import test from 'node:test';
import assert from 'node:assert/strict';
import {totals,sumMoney,csvCell} from '../src/lib/finance.mjs';
test('INR values sum precisely in paise',()=>assert.equal(sumMoney([{amount:.1},{amount:.2}],'amount'),.3));
test('Bill, collection and buyer balance remain distinct',()=>{
 const w={projects:[{id:'p1',budget:100000}],buyers:[{id:'b1',projectId:'p1',saleValue:80000}],payments:[{projectId:'p1',direction:'expense',amount:12000,paid:7500},{projectId:'p1',direction:'receipt',amount:20000,paid:20000}]};
 assert.deepEqual(totals(w),{budget:100000,committed:12000,spent:7500,collected:20000,sales:80000,payable:4500,receivable:60000,remaining:92500,net:12500});
 assert.equal(totals(w,'p2').spent,0);
});
test('CSV export quotes text and neutralizes formulas',()=>{assert.equal(csvCell('=1+1'),'"\'=1+1"');assert.equal(csvCell('a"b'),'"a""b"');});
