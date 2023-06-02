import { displayError, markEditorInfo } from './feedback'
import { download } from './downloadTree'


function handleHintModel(err, result) {
    console.debug(result)
    Session.set('is_hint_running', false)
    if (err) {
        return displayError(err)
    }

    if (result.alloy_hint) {
        console.debug(result.line - 1, result.column - 1, result.line2 - 1, result.column2 - 1)
        markEditorInfo(result.line - 1, result.column - 1, result.line2 - 1, result.column2 - 1)

        const log = `(${result.line}:${result.column}) - ${result.msg ?? 'No msg available.'}`
        console.debug(log)

        log_messages = Session.get('log-message')
        log_messages.push(log)
        Session.set('log-message', log_messages)

        log_classes = Session.get('log-class')
        log_classes.push('log-info')
        Session.set('log-class', log_classes)
    } else {
        log_messages = Session.get('log-message')
        log_messages.push(result.msg ?? 'Unable to generate hint')
        Session.set('log-message', log_messages)

        log_classes = Session.get('log-class')
        log_classes.push('log-info')
        Session.set('log-class', 'log-unavailable')
    }
}

export function hintModel() {
    Session.set('is_hint_running', true)

    if (Session.get('hint-enabled')) {
        Meteor.call('getHint', Session.get('last_id'), handleHintModel)
        Session.set('hint-enabled', false)
    }
}
