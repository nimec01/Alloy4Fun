import { chai, assert } from 'meteor/practicalmeteor:chai';
import { isParagraph } from './text';
/**
 * Default meteor tests for programming principles to be forced
 */
describe('editor text util functions', () => {
    it('identifies valid paragraphs', () => {
        chai.assert.isFalse(isParagraph('one sig'));
        chai.assert.isFalse(isParagraph('lol one sig'));
    });
    it('identifies invalid paragraphs', () => {
        chai.assert.isFalse(isParagraph('one sig'));
        chai.assert.isFalse(isParagraph('lol one sig'));
    });
});
