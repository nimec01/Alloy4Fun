generalSettings = (function generalSettings() {

    let currentLayout = 'breadthfirst'
    let useOriginalAtomNames = false
    let metaPrimSigs = [{ type: 'univ', parent: null }]
    // stores the parent prim sig of each sub sig
    let metaSubsetSigs = []

    function init(settings) {
        useOriginalAtomNames = settings.useOriginalAtomNames || []
        metaPrimSigs = settings.metaPrimSigs || [{ type: 'univ', parent: null }]
        metaSubsetSigs = settings.metaSubsetSigs || []
    }

    function data() {
        const data = { useOriginalAtomNames: useOriginalAtomNames,
              metaPrimSigs: metaPrimSigs,
            metaSubsetSigs: metaSubsetSigs }
        return data
    }

    function addPrimSig(tp,pr) {
        metaPrimSigs.push({
            type: tp,
            parent: pr
        })
    }

    function addSubSig(tp,pr) {
        metaSubsetSigs.push({
            type: tp,
            parent: pr
        })
    }

    function setOriginalAtomNamesValue(value) {
        if (value) {
            $('#atomLabelSettings').prop('disabled', true)
            var nodes = cy.nodes()
            nodes.forEach((node) => {
                const originalName = node.data().id.split('$')[0]
                node.data().label = originalName
                node.data().dollar = '$'
            })
        } else {
            $('#atomLabelSettings').prop('disabled', false)
            var nodes = cy.nodes()
            nodes.forEach((node) => {
                node.data().label = atomSettings.getAtomLabel(node.data().type)
                node.data().dollar = ''
            })
        }
    }
    
    function getLayout() {
        return currentLayout
    }
    
    function updateLayout(value) {
        currentLayout = value
    }  
    
    function getUseOriginalAtomNames() {
        return useOriginalAtomNames
    }
    
    function updateOriginalAtomNames(value) {
        useOriginalAtomNames = value
    }  

    function resetHierarchy() {
        metaPrimSigs = [{ type: 'univ', parent: null }]
        metaSubsetSigs = []
    }
    
    function getSigParent(sigType) {
        for (const i in metaPrimSigs) {
            if (metaPrimSigs[i].type == sigType) return metaPrimSigs[i].parent
        }
        for (const i in metaSubsetSigs) {
            if (metaSubsetSigs[i].type == sigType) return metaSubsetSigs[i].parent
        }
        throw null
    }

    function updateElementSelectionContent () {
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
            if ($.inArray(edge.data().relation, relations) == -1) relations.push(edge.data().relation)
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
    
    function hasSubsetSig(subsetSig) {
        for (let i = 0; i < metaSubsetSigs.length; i++) {
            if (metaSubsetSigs[i].type == subsetSig) return true
        }
        return false
    }

    return {
        init,
        data,
        getLayout,
        updateLayout,
        setOriginalAtomNamesValue,
        updateOriginalAtomNames,
        getUseOriginalAtomNames,
        getSigParent,
        resetHierarchy,
        updateElementSelectionContent,
        hasSubsetSig,
        addPrimSig,
        addSubSig
    }
}())