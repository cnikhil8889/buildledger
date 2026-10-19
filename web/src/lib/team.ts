import {android} from './native';
export function teamId(){
 if(typeof crypto.randomUUID==='function')return crypto.randomUUID();
 const bytes=crypto.getRandomValues(new Uint8Array(16));bytes[6]=(bytes[6]&15)|64;bytes[8]=(bytes[8]&63)|128;
 const hex=Array.from(bytes,b=>b.toString(16).padStart(2,'0')).join('');return `${hex.slice(0,8)}-${hex.slice(8,12)}-${hex.slice(12,16)}-${hex.slice(16,20)}-${hex.slice(20)}`;
}
/** Native HTTPS keeps the session token outside the WebView. */
export function teamRequest<T=Record<string,any>>(request:Record<string,unknown>):Promise<T>{
 return new Promise((resolve,reject)=>{
  const bridge=android();if(!bridge.teamRequest){reject(new Error('Install the latest BuildLedger team update.'));return;}
  const id=teamId();let timer:ReturnType<typeof setTimeout>;
  const finish=()=>{clearTimeout(timer);window.removeEventListener('buildledger-team-response',receive);};
  const receive=(event:Event)=>{const detail=(event as CustomEvent).detail;if(detail?.id!==id)return;finish();if(detail.result?.error)reject(new Error(detail.result.error));else resolve(detail.result as T);};
  window.addEventListener('buildledger-team-response',receive);
  timer=setTimeout(()=>{finish();reject(new Error('The response is taking too long. Refresh to check the result, or retry the same form.'));},125000);
  try{bridge.teamRequest(id,JSON.stringify(request));}catch(e){finish();reject(e);}
 });
}
export type TeamRecord=Record<string,any>;
export type TeamData={companies:TeamRecord[];company:string|null;role?:'owner'|'office'|'worker';userId?:string;members?:TeamRecord[];projects?:TeamRecord[];assignments?:TeamRecord[];tasks?:TeamRecord[];reports?:TeamRecord[];rates?:TeamRecord[];attendance?:TeamRecord[];wages?:TeamRecord[];payroll?:TeamRecord[];materials?:TeamRecord[];stock?:TeamRecord[];movements?:TeamRecord[];requests?:TeamRecord[];invites?:TeamRecord[];audit?:TeamRecord[]};
export function moneyPaise(value:string){if(!/^\d{1,9}(?:\.\d{1,2})?$/.test(value))throw new Error('Enter an amount in rupees with up to two decimal places.');const [rupees,paise='']=value.split('.');return Number(rupees)*100+Number(paise.padEnd(2,'0'));}
