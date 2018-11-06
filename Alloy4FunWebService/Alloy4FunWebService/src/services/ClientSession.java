package services;

import java.io.File;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4compiler.ast.ExprVar;
import edu.mit.csail.sdg.alloy4compiler.ast.Sig;
import edu.mit.csail.sdg.alloy4compiler.ast.Sig.Field;
import edu.mit.csail.sdg.alloy4compiler.ast.Sig.PrimSig;
import edu.mit.csail.sdg.alloy4compiler.ast.Sig.SubsetSig;
import edu.mit.csail.sdg.alloy4compiler.translator.A4Solution;
import edu.mit.csail.sdg.alloy4compiler.translator.A4Tuple;
import edu.mit.csail.sdg.alloy4compiler.translator.A4TupleSet;
import edu.mit.csail.sdg.alloy4viz.AlloyAtom;
import edu.mit.csail.sdg.alloy4viz.AlloyElement;
import edu.mit.csail.sdg.alloy4viz.AlloyInstance;
import edu.mit.csail.sdg.alloy4viz.AlloyProjection;
import edu.mit.csail.sdg.alloy4viz.AlloyRelation;
import edu.mit.csail.sdg.alloy4viz.AlloySet;
import edu.mit.csail.sdg.alloy4viz.AlloyTuple;
import edu.mit.csail.sdg.alloy4viz.AlloyType;
import edu.mit.csail.sdg.alloy4viz.StaticInstanceReader;
import edu.mit.csail.sdg.alloy4viz.StaticProjector;
import edu.mit.csail.sdg.alloy4viz.VizState;
import edu.mit.csail.sdg.alloy4viz.VizState.MString;
import kodkod.instance.Instance;
import kodkod.instance.TupleSet;
import kodkod.util.ints.IndexedEntry;

public class ClientSession {
	String sessid;
	int iteration;
	A4Solution ans;
	File model; 

	public ClientSession(A4Solution ans, File model, String sessid, int instanceNumber) {
		this.ans = ans;
		this.model = model;
		this.iteration = instanceNumber;
		this.sessid = sessid;
	}

	public String getInstance() {
		A4Solution aux = ans;
		try {
			for (int n = 0; n < this.iteration && aux.satisfiable(); n++) {
				aux = aux.next();
			}
		} catch (Exception e) {
			this.iteration--;
			return e.getMessage();
		}
		try {
			return answerToJson(aux);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public String projectOver(String[] type) {

		JsonArrayBuilder jsonResponseBuilder = Json.createArrayBuilder();
		try {
			
			String tmpPath = System.getProperty("java.io.tmpdir");
			String xmlPath = Paths.get(tmpPath, sessid + ".xml").toString();
			ans.writeXML(xmlPath);
			File xmlFile = new File(xmlPath);

			AlloyInstance myInstance = StaticInstanceReader.parseInstance(xmlFile);
			VizState myState = new VizState(myInstance);
			//clonamos o myState para obter um theme a usar abaixo para obter o originalName
			VizState theme= new VizState(myState);
			theme.useOriginalName(true);
			
			Map<AlloyType, AlloyAtom> map = new LinkedHashMap<AlloyType, AlloyAtom>();
			for (AlloyAtom alloy_atom : myState.getOriginalInstance().getAllAtoms()) {
				for (String projectingType : type) {					
					/*if (alloy_atom.toString().equals(projectingType) 
						|| projectingType.startsWith(alloy_atom.toString())
						)*/
					if (alloy_atom.getVizName(theme, true).replace("$","").equals(projectingType))
						map.put(alloy_atom.getType(), alloy_atom);
				}   
			}

			//subtitulo das relacoes com os atom projectados
			/*for (AlloyAtom alloy_atom : myState.getOriginalInstance().getAllAtoms()) {
				for(AlloyAtom map.values())
						map.put(alloy_atom.getType(), alloy_atom);
				   
			}*/

			
			
			
			AlloyProjection currentProjection = new AlloyProjection(map);
			AlloyInstance projected = StaticProjector.project(myInstance, currentProjection);
			jsonResponseBuilder.add(projectedInstance2JSON(projected));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return jsonResponseBuilder.build().toString();
	}

	private JsonObjectBuilder projectedInstance2JSON(AlloyInstance projected) {
		JsonObjectBuilder projectionsJSON = Json.createObjectBuilder();		

		VizState vs = new VizState(projected);
		vs.useOriginalName(true);

		JsonArrayBuilder jsonAtomsBuilder = Json.createArrayBuilder();
		//20180601
		JsonArrayBuilder jsonAtomsRelsBuilder = Json.createArrayBuilder(); 
		for (AlloyAtom a : projected.getAllAtoms()) {			
			jsonAtomsBuilder.add(a.getVizName(vs, true) );
			
			//20180601 relations to other atoms
			JsonObjectBuilder atomRel = Json.createObjectBuilder();
			atomRel.add("atom", a.getVizName(vs, true));
			JsonArrayBuilder jsonAtomRelsBuilder = Json.createArrayBuilder();
			List<AlloySet> sets = projected.atom2sets(a);
			if (sets!=null) {
				for(AlloySet set: sets) {
					jsonAtomRelsBuilder.add(set.getName());
				}				
			}
			atomRel.add("relations", jsonAtomRelsBuilder);
			jsonAtomsRelsBuilder.add(atomRel);
			//20180601
		}
			
		projectionsJSON.add("atoms", jsonAtomsBuilder);
		
		 //20180601objeto com os atomos e as relacaoes
		projectionsJSON.add("atom_rels", jsonAtomsRelsBuilder);
		
		JsonArrayBuilder jsonRelationsBuilder = Json.createArrayBuilder();
		for (AlloyRelation r : projected.model.getRelations()) {
			JsonObjectBuilder relationJsonBuilder = Json.createObjectBuilder();
			relationJsonBuilder.add("arity", r.getArity());
			relationJsonBuilder.add("relation", r.getName());

			JsonArrayBuilder relationTuplesJsonBuilder = Json.createArrayBuilder();
			for (AlloyTuple at : projected.relation2tuples(r)) {
				for (AlloyAtom atom : at.getAtoms()) {
					relationTuplesJsonBuilder.add(atom.getVizName(vs, true));
				}
			}
			relationJsonBuilder.add("tuples", relationTuplesJsonBuilder);
			jsonRelationsBuilder.add(relationJsonBuilder);
		}
		projectionsJSON.add("relations", jsonRelationsBuilder);
		
		return projectionsJSON;
	}

	public int getIteration() {
		return iteration;
	}

	public void setIteration(int iteration) {
		this.iteration = iteration;
	}

	public A4Solution getAns() {
		return ans;
	}

	public void deleteModel() {
		model.delete();
	}

	public String answerToJson(A4Solution answer) {
		JsonObjectBuilder instanceJSON = Json.createObjectBuilder();
		
		if (!answer.satisfiable()) {
			instanceJSON.add("unsat", "true");
			return instanceJSON.build().toString();
		}
			
		try {
			Instance sol = answer.debugExtractKInstance();
			instanceJSON.add("unsat", false);
			
			JsonArrayBuilder integersArrayJSON = Json.createArrayBuilder();
			for (IndexedEntry<TupleSet> e : sol.intTuples()) {
				Object atom = e.value().iterator().next().atom(0);
				integersArrayJSON.add(atom.toString());
			}
			instanceJSON.add("integers", integersArrayJSON);
			
			JsonArrayBuilder atomsJSON = Json.createArrayBuilder();
			JsonArrayBuilder fieldsJSON = Json.createArrayBuilder();

			for (Sig signature : answer.getAllReachableSigs()) {
				atomsJSON.add(sigToJSON(answer, signature));
				
				for (Field field : signature.getFields()) {
					fieldsJSON.add(fieldToJSON(answer, signature, field));
				}
			}
			instanceJSON.add("atoms", atomsJSON);
			instanceJSON.add("fields", fieldsJSON);

			instanceJSON.add("skolem", skolemsToJSON(answer));
			
			return instanceJSON.build().toString();
		} catch (Err er) {
			JsonObjectBuilder errorJSON = Json.createObjectBuilder();
			errorJSON.add("err", String.format("Evaluator error occurred: %s", er));
			return errorJSON.build().toString();
		}
	}

	private JsonObjectBuilder skolemsToJSON(A4Solution answer) throws Err {
		JsonObjectBuilder skolemJSON = Json.createObjectBuilder();
		for(ExprVar var : answer.getAllSkolems()) {
			A4TupleSet tupleSet = (A4TupleSet)answer.eval(var);
			JsonArrayBuilder varTuplesJSON = Json.createArrayBuilder();
			for(A4Tuple tuple: tupleSet) {
				varTuplesJSON.add(tupleToJSONArray(tuple));
			}
			skolemJSON.add(var.label, varTuplesJSON);
		}
		return skolemJSON;
	}

	private JsonObjectBuilder fieldToJSON(A4Solution answer, Sig signature, Field field) {
		JsonObjectBuilder fieldJSON = Json.createObjectBuilder();
		fieldJSON.add("type", signature.toString());
		fieldJSON.add("label", field.label);
		
		Iterator<A4Tuple> tupleIt = answer.eval(field).iterator();
		if(tupleIt.hasNext()) {
			A4Tuple tuple = tupleIt.next();
			fieldJSON.add("arity", tuple.arity());
			
			JsonArrayBuilder tupleValuesJSON = Json.createArrayBuilder();
			tupleValuesJSON.add(tupleToJSONArray(tuple));
			while(tupleIt.hasNext())tupleValuesJSON.add(tupleToJSONArray(tupleIt.next()));
			fieldJSON.add("values", tupleValuesJSON);
		}
		else {
			fieldJSON.add("values", Json.createArrayBuilder());
		}
		
		return fieldJSON;
	}

	private JsonObjectBuilder sigToJSON(A4Solution answer, Sig signature) {
		JsonObjectBuilder atomJSON = Json.createObjectBuilder();
		atomJSON.add("type", signature.toString());
		atomJSON.add("isSubsetSig", signature instanceof SubsetSig);
		
		String parent = "";
		if(signature instanceof PrimSig) {
			PrimSig primSignature = (PrimSig)signature;
			if(primSignature.parent != null) {
				parent = primSignature.parent.label;
			}
			else parent = "null";
		}
		atomJSON.add("parent", parent);
		
		JsonArrayBuilder atomParentsJSON = Json.createArrayBuilder();
		if(signature instanceof SubsetSig) {
			SubsetSig subsetSignature = (SubsetSig) signature;
			for(Sig subsetSigParent : subsetSignature.parents) {
				atomParentsJSON.add(subsetSigParent.label);
			}
		}
		atomJSON.add("parents", atomParentsJSON);
		atomJSON.add("isPrimSig", signature instanceof PrimSig);
		
		JsonArrayBuilder instancesJSON = Json.createArrayBuilder();
		for(A4Tuple tuple : answer.eval(signature)) {
			instancesJSON.add(tuple.atom(0));
		}
		atomJSON.add("values", instancesJSON);
		
		return atomJSON;
	}

	public JsonArrayBuilder tupleToJSONArray(A4Tuple tuple) {
		JsonArrayBuilder tupleJSON = Json.createArrayBuilder();
		for (int i = 0; i < tuple.arity(); i++)
			tupleJSON.add( tuple.atom(i));
		return tupleJSON;
	}
}