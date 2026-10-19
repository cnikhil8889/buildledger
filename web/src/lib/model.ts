import { z } from "zod";
export const folders = ["Project details", "Legal & approvals", "30-year title history", "Engineer & architect drawings", "Buyer agreements & sale", "Budget & estimates", "Payment receipts", "Project reports"] as const;
export const categories = ["Labour / workers", "Paint materials", "Painter labour", "Steel", "Cement", "Bricks", "Granite", "Tiles", "Electrical labour", "Wiring & electrical materials", "Legal fee (optional)", "Extra payments", "Other"] as const;
export const documentTypes = ["Title deed / ownership", "30-year link documents", "Encumbrance certificate", "Development agreement / GPA", "Land-use / conversion approval", "Sanctioned plan / layout", "Commencement permission", "RERA registration (if applicable)", "NOCs (as applicable)", "Completion / occupancy certificate", "Civil / structural drawing", "Architectural drawing", "Buyer agreement", "Sale deed", "Budget / BOQ", "Project reference", "Invoice / receipt", "Other"] as const;
const id = z.string().min(1).max(100);
const money = z.number().finite().nonnegative().max(1e12).refine(n => Math.abs(n * 100 - Math.round(n * 100)) < 0.001, "Use at most two decimal places");
const date = z.string().regex(/^\d{4}-\d{2}-\d{2}$/).refine(s => {const d=new Date(s);return !isNaN(d.getTime()) && d.toISOString().slice(0,10)===s;}, "Invalid date");
const projectSchema = z.object({id, name:z.string().trim().min(2).max(100), year:z.number().int().min(1990).max(2100), location:z.string().max(120), state:z.string().max(80), type:z.enum(["Residential", "Commercial", "Villa", "Plotted development"]), budget:money, progress:z.number().int().min(0).max(100), status:z.enum(["Planning", "In progress", "Completed", "On hold"]), createdAt:date, notes:z.string().max(2000), requirements:z.array(z.object({id,label:z.string().min(1).max(200),status:z.enum(["Pending", "Uploaded", "Not applicable"])})).max(60)});
const buyerSchema=z.object({id,projectId:id,name:z.string().trim().min(2).max(100),unit:z.string().min(1).max(60),phone:z.string().max(25),saleValue:money,date,notes:z.string().max(1000)});
const paymentSchema=z.object({id,projectId:id,direction:z.enum(["expense","receipt"]),category:z.string().min(1).max(80),party:z.string().trim().min(2).max(100),amount:money,paid:money,date,dueDate:z.union([date,z.literal("")]),method:z.enum(["Bank transfer","UPI","Cheque","Cash"]),reference:z.string().max(100),buyerId:z.string().max(100),notes:z.string().max(1000)}).refine(p=>p.paid<=p.amount,{message:"Paid amount cannot exceed the total",path:["paid"]});
export const workspaceSchema=z.object({builderName:z.string().trim().min(2).max(100),city:z.string().max(100),storage:z.literal("cloud"),projects:z.array(projectSchema).max(200),buyers:z.array(buyerSchema).max(3000),payments:z.array(paymentSchema).max(10000)}).superRefine((w,ctx)=>{
  const projects=new Set(w.projects.map(p=>p.id)), buyers=new Map(w.buyers.map(b=>[b.id,b]));
  if(projects.size!==w.projects.length || buyers.size!==w.buyers.length || new Set(w.payments.map(p=>p.id)).size!==w.payments.length) ctx.addIssue({code:"custom",message:"Duplicate record ID"});
  for(const b of w.buyers) if(!projects.has(b.projectId))ctx.addIssue({code:"custom",message:"Buyer project does not exist"});
  for(const p of w.payments) {
    if(!projects.has(p.projectId))ctx.addIssue({code:"custom",message:"Payment project does not exist"});
    if(p.direction==="expense" && !categories.includes(p.category as typeof categories[number]))ctx.addIssue({code:"custom",message:"Invalid expense category"});
    if(p.direction==="receipt" && (!buyers.has(p.buyerId)||buyers.get(p.buyerId)?.projectId!==p.projectId))ctx.addIssue({code:"custom",message:"Select a buyer belonging to this project"});
  }
});
export type Workspace=z.infer<typeof workspaceSchema>;
export type Project=Workspace["projects"][number];
export type Payment=Workspace["payments"][number];
export type Buyer=Workspace["buyers"][number];
export type StoredFile={id:string;projectId:string;folder:string;documentType:string;name:string;size:number;createdAt:string};
export const defaultRequirements=()=>documentTypes.slice(0,10).map((label,i)=>({id:`req-${i}`,label,status:"Pending" as const}));
export const today=()=>new Date().toLocaleDateString("en-CA",{timeZone:"Asia/Kolkata"});
