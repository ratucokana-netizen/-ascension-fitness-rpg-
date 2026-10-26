// Supabase Edge Function: analyze-food
// Deploy with: supabase functions deploy analyze-food
// Set OPENAI_API_KEY as a Supabase secret, NEVER in the APK.
const cors={"Access-Control-Allow-Origin":"*","Access-Control-Allow-Headers":"authorization, x-client-info, apikey, content-type","Access-Control-Allow-Methods":"POST, OPTIONS"};
const reply=(obj,status=200)=>new Response(JSON.stringify(obj),{status,headers:{...cors,"Content-Type":"application/json"}});
Deno.serve(async(req)=>{
 if(req.method==="OPTIONS")return new Response(null,{headers:cors});
 if(req.method!=="POST")return reply({error:"Method not allowed"},405);
 try{
  const jwt=req.headers.get("Authorization")?.replace(/^Bearer\s+/i,"");
  const url=Deno.env.get("SUPABASE_URL"),anon=Deno.env.get("SUPABASE_ANON_KEY"),key=Deno.env.get("OPENAI_API_KEY");
  if(!url||!anon||!key)return reply({error:"Image analysis service is not configured"},503);
  if(!jwt)return reply({error:"Sign in to Guild before analyzing a photo"},401);
  const auth=await fetch(url+"/auth/v1/user",{headers:{apikey:anon,Authorization:"Bearer "+jwt}});
  if(!auth.ok)return reply({error:"Your sign-in has expired. Sign in again."},401);
  const user=await auth.json();if(!user.id)return reply({error:"Authentication required"},401);
  const body=await req.json();const data=body.image;
  if(typeof data!=="string"||data.length>6_500_000||!/^data:image\/(jpeg|png|webp);base64,[a-zA-Z0-9+/=]+$/.test(data))return reply({error:"Provide a JPEG, PNG, or WebP photo under 4 MB"},400);
  const prompt="You are a cautious nutrition label reader and meal estimator. Read the image. If a readable nutrition label is visible, extract nutrition facts and the serving basis; if the label is per 100g and the consumed portion is unknown, do not invent a serving size. If this is a meal photo, identify plausible food components and provide an approximate per-plate estimate, noting that weight and cooking oils are uncertain. Never claim exact accuracy. Return a single JSON object with: food_name (string), kind (label, meal, unknown), calories (number|null), protein (number|null), carbs (number|null), fat (number|null), serving_description (string), confidence (low, medium, high), assumptions (string[]), needs_confirmation (boolean). Only include numeric macros when an amount can be reasonably estimated. If image is not food or a nutrition label, set kind unknown and macros null. No markdown.";
  const ai=await fetch("https://api.openai.com/v1/responses",{method:"POST",headers:{Authorization:"Bearer "+key,"Content-Type":"application/json"},body:JSON.stringify({model:"gpt-4.1-mini",store:false,max_output_tokens:450,input:[{role:"user",content:[{type:"input_text",text:prompt},{type:"input_image",image_url:data}]}],text:{format:{type:"json_schema",name:"nutrition_estimate",strict:true,schema:{type:"object",additionalProperties:false,properties:{food_name:{type:"string"},kind:{type:"string",enum:["label","meal","unknown"]},calories:{type:["number","null"]},protein:{type:["number","null"]},carbs:{type:["number","null"]},fat:{type:["number","null"]},serving_description:{type:"string"},confidence:{type:"string",enum:["low","medium","high"]},assumptions:{type:"array",items:{type:"string"}},needs_confirmation:{type:"boolean"}},required:["food_name","kind","calories","protein","carbs","fat","serving_description","confidence","assumptions","needs_confirmation"]}}}})});
  const result=await ai.json();
  if(!ai.ok)return reply({error:"Image analysis unavailable. Please try again or enter macros manually."},502);
  const output=(result.output||[]).flatMap(x=>x.content||[]).find(x=>x.type==="output_text")?.text;
  if(!output)return reply({error:"No nutrition estimate returned"},502);
  const parsed=JSON.parse(output);
  for(const k of ["calories","protein","carbs","fat"])if(parsed[k]!==null&&(!Number.isFinite(parsed[k])||parsed[k]<0||parsed[k]>10000))parsed[k]=null;
  return reply({result:parsed});
 }catch(e){return reply({error:"Could not analyze this photo. Please try again or enter macros manually."},500)}
});