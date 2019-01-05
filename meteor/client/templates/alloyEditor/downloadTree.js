export {
    downloadTree
}

function downloadTree() {
    console.log("downloading");
    Meteor.call("downloadTree", Session.get("last_id"), () => {
		
    })
}