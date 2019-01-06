import {
    displayError
} from "../../lib/editor/feedback"

function downloadTree() {
    Meteor.call("downloadTree", Router.current().data()._id, (err, res) => {
        if (err) return displayError(err)
        console.log(descendantsToTree(res));
        return descendantsToTree(res)
    })
}

function descendantsToTree(res) {
    console.log(res);
    let descendants = res.descendants
    let root = res.root
    // get all the ids
    let ids = descendants.map(x => x._id)
    ids.push(root._id)
    // generate a hashmap of id -> direct child
    let hashmap = {}
    ids.forEach(id => hashmap[id] = []);
    descendants.forEach(model => {
        hashmap[model.derivationOf].push(model)
    });
    //depth first search to obtain recursive tree structure
    let current, queue = [root]
    while (queue.length) {
        current = queue.shift()
        current.children = current.children || []
        hashmap[current._id].forEach(model => {
            queue.push(model)
            current.children.push(model)
        });
    }
    return root
}
export {
    downloadTree,
    descendantsToTree
}