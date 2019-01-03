import assert from 'assert';

/**
 * Default meteor tests for programming principles to be forced
 */
describe('meteor_docker', () => {
    it('package.json has correct name', async () => {
        const {
            name,
        } = await import('../package.json');
        assert.strictEqual(name, 'meteor_docker');
    });

    if (Meteor.isClient) {
        it('client is not server', () => {
            assert.strictEqual(Meteor.isServer, false);
        });
    }

    if (Meteor.isServer) {
        it('server is not client', () => {
            assert.strictEqual(Meteor.isClient, false);
        });
        // it("should access the API", function() {
        //     Meteor.setTimeout(function() {
        //         HTTP.call('GET', 'http://0.0.0.0:8080/greet', {
        //             "options": "to set"
        //         }, function(_, response) {
        //             console.log("API IS WORKING: " + response.content);
        //         });
        //     }, 5000)
        // })
    }
});
