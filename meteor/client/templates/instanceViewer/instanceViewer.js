/**
 * This template is used for displaying the counter-example instances
 * when models are not satisified
 */



function getCurrentInstance(instanceNumber) {
    var instances = Session.get("instances");
    var result = undefined;
    instances.forEach(function(inst) {
        if (inst.number == instanceNumber) {
            result = inst;
            return;
        }
    });
    return result;
}