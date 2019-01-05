import {
    chai,
    assert
} from 'meteor/practicalmeteor:chai';
import {
    containsValidSecretWithAnonymousCommand
} from "./genUrl"
/**
 * Default meteor tests for programming principles to be forced
 */
describe("gen url submethods work", function() {
    it("identifies anonymous methods", function() {
        chai.assert.isFalse(containsValidSecretWithAnonymousCommand("//SECRET\ncheck test{}"))
        chai.assert.isTrue(containsValidSecretWithAnonymousCommand("//SECRET\ncheck{}"))
    });
});