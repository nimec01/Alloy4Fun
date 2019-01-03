/**
 * This template is used for displaying the counter-example instances
 * when models are not satisified
 */


function getCurrentInstance(instanceNumber) {
    const instances = Session.get('instances');
    let result;
    instances.forEach((inst) => {
        if (inst.number == instanceNumber) {
            result = inst;
        }
    });
    return result;
}
