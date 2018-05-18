package services;

import java.io.File;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
import edu.mit.csail.sdg.alloy4viz.AlloyAtom;
import edu.mit.csail.sdg.alloy4viz.AlloyInstance;
import edu.mit.csail.sdg.alloy4viz.AlloyProjection;
import edu.mit.csail.sdg.alloy4viz.AlloyRelation;
import edu.mit.csail.sdg.alloy4viz.AlloyTuple;
import edu.mit.csail.sdg.alloy4viz.AlloyType;
import edu.mit.csail.sdg.alloy4viz.StaticInstanceReader;
import edu.mit.csail.sdg.alloy4viz.StaticProjector;
import edu.mit.csail.sdg.alloy4viz.VizState;
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
			return toJson(aux);
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
			Map<AlloyType, AlloyAtom> map = new LinkedHashMap<AlloyType, AlloyAtom>();
			for (AlloyAtom alloy_atom : myState.getOriginalInstance().getAllAtoms()) {
				for (String projectingType : type) {
					if (alloy_atom.toString().equals(projectingType))
						map.put(alloy_atom.getType(), alloy_atom);
				}
			}
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
		for (AlloyAtom a : projected.getAllAtoms())
			jsonAtomsBuilder.add(a.getVizName(vs, true));
		projectionsJSON.add("atoms", jsonAtomsBuilder);
		
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

	public String toJson(A4Solution answer) {

		if (!answer.satisfiable())
			return "{\"unsat\" : \"true\"}";
		try {

			Instance sol = answer.debugExtractKInstance();
			StringBuilder sb = new StringBuilder();
			sb.append("{\"unsat\" : false , \"integers\" : [");
			boolean firstTuple = true;
			for (IndexedEntry<TupleSet> e : sol.intTuples()) {
				if (firstTuple)
					firstTuple = false;
				else
					sb.append(", ");
				Object atom = e.value().iterator().next().atom(0);
				sb.append(atom);
			}
			sb.append("], ");
			String atoms = "[", fields = "[";
			boolean hasAtoms = false;
			boolean hasFields = false;

			for (Sig s : answer.getAllReachableSigs()) {
				if (hasAtoms)
					atoms += ", ";
				atoms += "{\"type\" : \"" + s + "\",\"isSubsetSig\" : " + (s instanceof SubsetSig) + ", \"parent\" : \""
						+ ((s instanceof PrimSig) ? ((PrimSig) s).parent : "") + "\", \"parents\" : "
						+ ((s instanceof SubsetSig) ? ((SubsetSig) s).parents.toString().replace("[", "[\"")
								.replace("]", "\"]").replace(",", "\",\"") : "[]")
						+ ", \"isPrimSig\" : " + (s instanceof PrimSig) + ", \"values\" : [";
				Iterator<A4Tuple> it = answer.eval(s).iterator();
				while (it.hasNext()) {
					atoms += "\"" + it.next() + "\"";
					if (it.hasNext())
						atoms += " , ";
				}
				atoms += "]}";
				hasAtoms = true;

				for (Field f : s.getFields()) {

					if (hasFields)
						fields += ", ";
					fields += "{\"type\" : \"" + s + "\", \"label\" : \"" + f.label + "\"";
					it = answer.eval(f).iterator();
					if (it.hasNext()) {
						A4Tuple tuple = it.next();

						fields += ", \"arity\" : " + tuple.arity();
						fields += ",  \"values\" : [" + tupleToJSON(tuple) + "";
						if (it.hasNext())
							fields += ", ";
					} else
						fields += ",  \"values\" : [";
					while (it.hasNext()) {
						A4Tuple tuple = it.next();
						fields += tupleToJSON(tuple);
						if (it.hasNext())
							fields += ", ";
					}
					fields += "]}";
					hasFields = true;
				}
			}
			atoms += "]";
			fields += "]";
			sb.append("\"atoms\" : " + atoms);
			sb.append(", \"fields\" : " + fields);

			sb.append(", \"skolem\" : {");
			Iterator<ExprVar> vars = answer.getAllSkolems().iterator();
			while (vars.hasNext()) {
				ExprVar v = vars.next();
				// for (ExprVar v : answer.getAllSkolems()) {
				sb.append("\"" + v.label + "\"").append(" : ")
						.append(answer.eval(v).toString().replace("{", "\"").replace("}", "\""));
				if (vars.hasNext())
					sb.append(", ");
			}
			sb.append("}}");
			return sb.toString();
		} catch (Err er) {
			return ("{\"err\" : \"Evaluator error occurred: " + er + "\"}");
		}
	}

	public String tupleToJSON(A4Tuple tuple) {
		String result = "[";
		int i;
		for (i = 0; i < tuple.arity() - 1; i++)
			result += "\"" + tuple.atom(i) + "\", ";
		result += "\"" + tuple.atom(i) + "\"";
		result += "]";

		return result;
	}
}
