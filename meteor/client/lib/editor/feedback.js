function displayError(err) {
    swal({
        type: 'error',
        title: err.error,
        text: err.reason
    })
    return err
}

export {
    displayError
}