Meteor.methods({
    
    /**
      * Meteor method to get the projection of an Alloy instance. This will
      * call the Alloy API (webService).
      *
      * @param uuid id of the session
      * @param frameInfo object with information about the frame
      *
      * @return JSON object with the projection
      */
    getProjection: function(uuid, frameInfo) {
        let type = [];
        for (var key in frameInfo) {
            type.push(key + frameInfo[key]);
        };
        return new Promise((resolve, reject) => {
            HTTP.call('POST', `${Meteor.settings.env.API_URL}/getProjection`, {
                data: {
                    uuid: uuid,
                    type: type
                }
            }, (error, result) => {
                if (error) reject(error)
                let content = JSON.parse(result.content)
                if (content.unsat) {
                    content.check = true;
                } else {
                    Object.keys(content).forEach(k => {
                        content[k].check = true;
                    });
                }
                resolve(content);

            });
        })
    }
});
