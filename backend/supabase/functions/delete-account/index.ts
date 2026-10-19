// Deploy with JWT verification disabled at the gateway for publishable-key projects;
// this function validates the bearer token with Auth before every operation.
import {createClient} from 'npm:@supabase/supabase-js@2.118.0';
Deno.serve(async (request:Request) => {
  if(request.method!=='POST')return new Response('{}',{status:405});
  const url=Deno.env.get('SUPABASE_URL')!,adminKey=Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!;
  const bearer=request.headers.get('Authorization')||'';
  if(!bearer.startsWith('Bearer '))return new Response('{}',{status:401});
  const admin=createClient(url,adminKey,{auth:{persistSession:false,autoRefreshToken:false}});
  const {data,error}=await admin.auth.getUser(bearer.slice(7));
  if(error||!data.user)return new Response('{}',{status:401});
  const {error:signOutError}=await admin.auth.admin.signOut(bearer.slice(7),'global');
  if(signOutError)return new Response('{}',{status:503});
  const prefix=data.user.id+'/BuildLedger',bucket=admin.storage.from('buildledger');
  // Remove objects through the Storage API, not SQL, so the underlying bytes are deleted.
  for(const folder of [prefix+'/snapshots',prefix]){
    for(let pages=0;pages<100;pages++){
      const {data:items,error:listError}=await bucket.list(folder,{limit:100});
      if(listError)return new Response('{}',{status:503});
      const paths=(items||[]).filter(item=>item.id).map(item=>folder+'/'+item.name);
      if(!paths.length)break;
      const {error:removeError}=await bucket.remove(paths);
      if(removeError)return new Response('{}',{status:503});
    }
  }
  const {error:deleteError}=await admin.auth.admin.deleteUser(data.user.id);
  return new Response('{}',{status:deleteError?503:200,headers:{'Content-Type':'application/json','Cache-Control':'no-store'}});
});
