generalSettings = (function generalSettings() {
    let currentLayout = 'breadthfirst'
    let metaPrimSigs = [{ type: 'univ', parent: null }]
    // stores the parent prim sig of each sub sig
    let metaSubsetSigs = []

    /**
     * Initialize general settings structures.
     */
    function init(settings) {
        currentLayout = (settings && settings.currentLayout) || 'breadthfirst'
        if (settings) {
            metaPrimSigs = settings.metaPrimSigs || [{ type: 'univ', parent: null }]
            metaSubsetSigs = settings.metaSubsetSigs || []
        }
    }

    /**
     * Export general settings structures as object.
     */
    function data() {
        const data = { 
            currentLayout,
            metaPrimSigs,
            metaSubsetSigs }
        return data
    }

    /**
     * Add a new prim sig to the settings with a given parent.
     *
     * @param tp {String} the new prim sig
     * @param pr {String} the parent sig
     */
    function addPrimSig(tp, pr) {
        metaPrimSigs.push({
            type: tp,
            parent: pr
        })
    }

    /**
     * Add a new sub sig to the settings with a given parent.
     *
     * @param tp {String} the new sub sig
     * @param pr {String} the parent sig
     */
    function addSubSig(tp, pr) {
        metaSubsetSigs.push({
            type: tp,
            parent: pr
        })
    }

    /**
     * Retrieves the current layout property, initialized as breadthfirst.
     *
     * @returns {String} the value assigned to the property
     */
    function getLayout() {
        return currentLayout
    }

    /**
     * Updates the current layout property.
     *
     * @param {String} newVal the new value for the property
     */
    function updateLayout(value) {
        currentLayout = value
    }

    /**
     * Resets the known hierarchy for a new instance.
     */
    function resetHierarchy() {
        metaPrimSigs = [{ type: 'univ', parent: null }]
        metaSubsetSigs = []
    }

    /**
     * Retrieves the parent of a prim or sub sig.
     *
     * @param {String} the sig for which to find the parent
     */
    function getSigParent(sigType) {
        for (const i in metaPrimSigs) {
            if (metaPrimSigs[i].type === sigType) return metaPrimSigs[i].parent
        }
        for (const i in metaSubsetSigs) {
            if (metaSubsetSigs[i].type === sigType) return metaSubsetSigs[i].parent
        }
        throw null
    }

    /**
     * Whether a sig has sub sigs.
     *
     * @param {String} the sig for which to children
     */
    function hasSubsetSig(subsetSig) {
        for (let i = 0; i < metaSubsetSigs.length; i++) {
            if (metaSubsetSigs[i].type === subsetSig) return true
        }
        return false
    }

    /**
     * Updates the content of the side bar property settings.
     *
     * NOTE: should not be here but will be dropped for v1.3.
     */
    function updateElementSelectionContent() {
        // var nodes = cy.nodes();
        const edges = cy.edges()
        const types = []
        const subsets = []
        const relations = []
        // Gather all distinct types from nodes represented in the graph
        metaPrimSigs.forEach((sig) => {
            if ($.inArray(sig.type, types) == -1) types.push(sig.type)
        })
        // get all distinct subset signatures
        metaSubsetSigs.forEach((subsetSig) => {
            subsets.push(subsetSig.type)
        })
        // Gather all distinct relations from edges represented in the graph
        edges.forEach((edge) => {
            if ($.inArray(edge.data().relation, relations) == -1) {
                relations.push(edge.data().relation)
            }
        })

        // Remove previous types available for selection
        selectAtomElement.selectize.clear()
        selectAtomElement.selectize.clearOptions()

        // Remove previous subsets available for selection
        selectSubset.selectize.clear()
        selectSubset.selectize.clearOptions()

        // Remove previous relations available for selection
        selectRelationElement.selectize.clear()
        selectRelationElement.selectize.clearOptions()

        // Add new Types
        types.forEach((type) => {
            selectAtomElement.selectize.addOption({ value: type, text: type })
            selectAtomElement.selectize.addItem(type)
        })
        // Replace tag on the bottom right corner of type selection div
        $('.wrapper-select-atom > div > div.selectize-input > p').remove()
        $('.wrapper-select-atom > div > div.selectize-input').append("<p class='select-label'>Signatures</p>")


        // Add new Subsets
        subsets.forEach((subset) => {
            selectSubset.selectize.addOption({ value: subset, text: subset })
            selectSubset.selectize.addItem(subset)
        })
        // Replace tag on the bottom right corner of subset selection div
        $('.wrapper-select-subset > div > div.selectize-input > p').remove()
        $('.wrapper-select-subset > div > div.selectize-input').append("<p class='select-label'>Subsets</p>")

        // Add new Relations
        relations.forEach((relation) => {
            selectRelationElement.selectize.addOption({ value: relation, text: relation })
            selectRelationElement.selectize.addItem(relation)
        })
        // Replace tag on the bottom right corner of relation selection div
        $('.wrapper-select-relation > div > div.selectize-input > p').remove()
        $('.wrapper-select-relation > div > div.selectize-input').append("<p class='select-label'>Relations</p>")
    }

    return {
        init,
        data,
        getLayout,
        updateLayout,
        getSigParent,
        resetHierarchy,
        updateElementSelectionContent,
        hasSubsetSig,
        addPrimSig,
        addSubSig
    }
}())
