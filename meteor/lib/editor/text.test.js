import {
    chai,
    assert
} from 'meteor/practicalmeteor:chai';
import {
    isParagraph,
    containsValidSecret
} from "./text"
/**
 * Default meteor tests for programming principles to be forced
 */
describe("editor text util functions", function() {
    it("identifies invalid paragraphs", function() {
        chai.assert.isFalse(isParagraph("one sig"))
        chai.assert.isFalse(isParagraph("lol one sig A"))
    });
    it("identifies valid paragraphs", function() {
        chai.assert.isTrue(isParagraph("one sig A"))
        chai.assert.isTrue(isParagraph("pred X {}"))
    });
    it("identifies invalid secrets", function() {
        chai.assert.isFalse(containsValidSecret("something"))
        chai.assert.isFalse(containsValidSecret("something/SECRET"))
        chai.assert.isFalse(containsValidSecret("something//SECRET\n"))
    });
    it("identifies valid secrets", function() {
        chai.assert.isTrue(containsValidSecret("something\n//SECRET\nthis is the secret"))
        chai.assert.isTrue(containsValidSecret("\n//SECRET\nthis is the secret"))
    });
});