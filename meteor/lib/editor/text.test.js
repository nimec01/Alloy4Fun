import { chai, assert } from 'meteor/practicalmeteor:chai';
import {
    isParagraph
} from "./text"
/**
 * Default meteor tests for programming principles to be forced
 */
describe("editor text util functions", function() {
    it("identifies valid paragraphs", function() {
        chai.assert.isFalse(isParagraph("one sig"))
        chai.assert.isFalse(isParagraph("lol one sig"))
    });
    it("identifies invalid paragraphs", function() {
        chai.assert.isFalse(isParagraph("one sig"))
        chai.assert.isFalse(isParagraph("lol one sig"))
    });
});