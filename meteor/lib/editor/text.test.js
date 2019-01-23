import {
    chai,
    assert
} from 'meteor/practicalmeteor:chai';
import {
    isParagraph,
    containsValidSecret,
    getCommandsFromCode,
    secretTag
} from "./text"
/**
 * Default meteor tests for programming principles to be forced
 */
describe("editor text util functions", function() {
    it("identifies invalid secrets", function() {
        chai.assert.isFalse(containsValidSecret("/*\n//SECRET\n*/\nsig a {}"))
        chai.assert.isFalse(containsValidSecret("//SECRET  \nsig a {}"))
        chai.assert.isFalse(containsValidSecret(" //SECRET\nsig a {}"))
        chai.assert.isFalse(containsValidSecret("something"))
        chai.assert.isFalse(containsValidSecret("something/SECRET"))
        chai.assert.isFalse(containsValidSecret("something//SECRET\n"))
    });
    it("identifies valid secrets", function() {
        chai.assert.isTrue(containsValidSecret("//SECRET\nsig a {}"))
        chai.assert.isTrue(containsValidSecret("something\n//SECRET\nthis is the secret"))
        chai.assert.isTrue(containsValidSecret("\n//SECRET\nthis is the secret"))
    });
    it("identifies correct commands in code", function() {
        let code = `
// run shouldNotDetect { no d: Dir | d in d.^contents }
// check shouldNotDetect2 { no d: Dir | d in d.^contents }
run run1 for 5
run run2
run run3 {}
check check1 for 5
check check2
`
        chai.assert.sameMembers(getCommandsFromCode(code), ["run run1", "run run2", "run run3", "check check1", "check check2"])
    });
});
