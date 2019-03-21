import cytoscape from 'cytoscape'
import { updateRightClickContent } from '../../templates/visSettings/rightClickMenu'
import { instChanged } from '../editor/state'
import { newInstanceSetup } from './projection'

updateGraph = function (instance) {
    // Remove previous nodes and edges.
    cy.remove(cy.elements())
    // Add new ones.
    const atomElements = getAtoms(instance)
    cy.add(atomElements)
    cy.add(getEdges(instance))
    Session.set('empty-instance', atomElements.length == 0)
    cy.resize()
    // Apply same theme settings as previous instance.
    applyThemeSettings()
    // Draw data according to the selected layout.
    applyCurrentLayout()
}

applyThemeSettings = function () {
    // Apply show as attributes option set on previous instances.
    generalSettings.setOriginalAtomNamesValue(!!generalSettings.getUseOriginalAtomNames())

    // In case of label change.
    refreshGraph()
    // Add types, subsets and relations to selection area on settings tab.
    generalSettings.updateElementSelectionContent()
    // Backup of whole instance. Helpful for projection.
    allNodes = cy.nodes()
    // Apply same projections as on previous instances.
    newInstanceSetup()
}


// Get atom information received from server ready to upload to cytoscape object.
getAtoms = function (instance) {
    const atoms = []
    generalSettings.resetHierarchy()
    if (instance.atoms) {
        instance.atoms.forEach((atom) => {
            if (atom.type == 'String') {
                generalSettings.addPrimSig(atom.type, atom.parent)
                sigSettings.getAtomBorder(atom.type)
                sigSettings.getAtomColor(atom.type)
                sigSettings.getAtomShape(atom.type)
                atom.values.forEach((value) => {
                    const type = value.substr(1, value.length - 2)
                    atoms.push(
                        {
                            group: 'nodes',
                            classes: 'multiline-manual',
                            data: {
                                number: value,
                                numberBackup: value,
                                color: sigSettings.getAtomColor(type),
                                shape: sigSettings.getAtomShape(type),
                                id: value,
                                type: 'String',
                                label: value,
                                dollar: '',
                                border: sigSettings.getAtomBorder(type),
                                subsetSigs: []
                            }
                        }
                    )
                    return atoms
                })
            } else if (atom.type.toLowerCase().indexOf('this/') > -1) {
                if (atom.isPrimSig) {
                    generalSettings.addPrimSig(atom.type.split('/')[1], atom.parent.indexOf('/') > -1 ? atom.parent.split('/')[1] : atom.parent)
                    sigSettings.getAtomBorder(atom.type.split('/')[1])
                    sigSettings.getAtomColor(atom.type.split('/')[1])
                    sigSettings.getAtomShape(atom.type.split('/')[1])

                    atom.values.forEach((value) => {
                        if (value.indexOf('/') == -1) var type = value.split('$')[0]
                        atoms.push(
                            {
                                group: 'nodes',
                                classes: 'multiline-manual',
                                data: {
                                    number: value.split('$')[1],
                                    numberBackup: value.split('$')[1],
                                    color: sigSettings.getAtomColor(type),
                                    shape: sigSettings.getAtomShape(type),
                                    id: value,
                                    type,
                                    label: sigSettings.getAtomLabel(type),
                                    dollar: '',
                                    border: sigSettings.getAtomBorder(type),
                                    subsetSigs: []
                                }
                            }
                        )
                        return atoms
                    })
                } else {
                    atom.values.forEach((value) => {
                        if (!generalSettings.hasSubsetSig(`${atom.type.split('/')[1]}:${value.split('$')[0]}`)) {
                            const type = `${atom.type.split('/')[1]}:${value.split('$')[0]}`
                            generalSettings.addSubSig(`${atom.type.split('/')[1]}:${value.split('$')[0]}`, value.split('$')[0])
                            sigSettings.getAtomBorder(type)
                            sigSettings.getAtomColor(type)
                            sigSettings.getAtomShape(type)
                            sigSettings.getAtomLabel(type)
                            sigSettings.updateAtomLabel(type, atom.type.split('/')[1])
                        }
                        for (let i = 0; i < atoms.length; i++) {
                            if (atoms[i].data.id == value) {
                                atoms[i].data.subsetSigs.push(`${atom.type.split('/')[1]}:${value.split('$')[0]}`)
                            }
                        }
                    })
                }

                return atoms
            }
        })
    }

    for (const skolem in instance.skolem) {
        for (const atom in atoms) {
            if (atoms[atom].data.id == instance.skolem[skolem]) {
                atoms[atom].data.skolem = skolem
            }
        }
    }
    return atoms
}

getEdges = function (instance) {
    const result = []
    if (instance.fields) {
        instance.fields.forEach((field) => {
            if (field.type.indexOf('this/') != -1) {
                field.values.forEach((relation) => {
                    const { label } = field
                    const labelExt = relation.slice(1, relation.length - 1).toString()
                    result.push({
                        group: 'edges',
                        selectable: true,
                        data: {
                            relation: label,
                            source: relation[0],
                            target: relation[relation.length - 1],
                            label: relationSettings.getEdgeLabel(label),
                            color: relationSettings.getEdgeColor(label),
                            // when relation's arity > 2, add remaining involved types to its label
                            labelExt: field.arity > 2 ? labelExt : '',
                            // useful when these types have their labels edited. "labelExt" is a backup of the original while "updatedLabelExt" reflects the current state of the world
                            updatedLabelExt: field.arity > 2 ? labelExt : '',
                            edgeStyle: relationSettings.getEdgeStyle(label)
                        }
                    })
                })
            }
        })
    }
    return result
}

// Selecting an element on the cytoscape canvas recalculates its label. Useful after editing labels
refreshGraph = function () {
    const selected = cy.$(':selected')
    cy.elements().select().unselect()
    selected.select()
}

initGraphViewer = function (element) {
    cy = cytoscape({
        container: document.getElementById(element), // container to render in
        elements: [ // list of graph elements to start with

        ],
        zoom: 1,
        pan: { x: 0, y: 0 },

        // interaction options:
        minZoom: 0.2,
        maxZoom: 5,
        wheelSensitivity: 0.5,

        style: [ // the stylesheet for the graph
            {
                selector: 'node',
                style: {
                    'background-color'(ele) {
                        let val
                        if (ele.data().subsetSigs.length > 0) {
                            // TODO: this randomly selects one of the subsigs (as does the AA)
                            val = sigSettings.getInheritedAtomColor(ele.data().subsetSigs[0])
                        } else val = sigSettings.getInheritedAtomColor(ele.data().type)
                        return val
                    },
                    label(ele) {
                        let val
                        if (ele.data().subsetSigs.length > 0) {
                        // TODO: this randomly selects one of the subsigs (as does the AA)
                            val = sigSettings.getInheritedDisplayNodesNumber(ele.data().subsetSigs[0])
                        } else val = sigSettings.getInheritedDisplayNodesNumber(ele.data().type)
                        let l
                        // if string atom, do not pre-pend the sig label
                        if (ele.data().label == 'String') l = ele.data().number
                        // whether to show numbers on labels
                        else { l = sigSettings.getAtomLabel(ele.data().type) + (val ? (ele.data().dollar + ele.data().number) : '') }
                        // subsig labels
                        const subsigs = ele.data().subsetSigs.length > 0 ? `\n(${ele.data().subsetSigs.map(sigSettings.getAtomLabel)})` : ''

                        // relations as attributes labels
                        relationSettings.propagateAttributes()

                        let attributes = ''
                        for (const i in ele.data().attributes) attributes += `\n${cy.edges(`[relation='${i}']`)[0].data().label} : ${ele.data().attributes[i].toString()}`

                        // skolem variable labels
                        const skolems = typeof ele.data().skolem !== 'undefined' ? `\n${ele.data().skolem}` : ''

                        return `${l}${subsigs}${attributes}${skolems}`
                    },
                    'border-style'(ele) {
                        let val
                        if (ele.data().subsetSigs.length > 0) val = sigSettings.getInheritedAtomBorder(ele.data().subsetSigs[0])
                        else { val = sigSettings.getInheritedAtomBorder(ele.data().type) }
                        return val
                    },
                    'text-valign': 'center',
                    'text-outline-color': 'black',
                    shape(ele) {
                        let val
                        if (ele.data().subsetSigs.length > 0) val = sigSettings.getInheritedAtomShape(ele.data().subsetSigs[0])
                        else { val = sigSettings.getInheritedAtomShape(ele.data().type) }
                        return val
                    },
                    visibility(ele) {
                        let val1
                        let val2
                        if (ele.data().subsetSigs.length > 0) {
                            val1 = sigSettings.getInheritedAtomVisibility(ele.data().subsetSigs[0])
                            val2 = sigSettings.getInheritedHideUnconnectedNodes(ele.data().subsetSigs[0])
                        } else {
                            val1 = sigSettings.getInheritedAtomVisibility(ele.data().type)
                            val2 = sigSettings.getInheritedHideUnconnectedNodes(ele.data().type)
                        }
                        if (val2) val2 = ele.neighbourhood().length == 0
                        return val1 || val2 ? 'hidden' : 'visible'
                    },
                    width: 'label',
                    height: 'label',
                    'padding-bottom': '10px',
                    'padding-top': '10px',
                    'padding-left': '10px',
                    'padding-right': '10px',
                    'border-color': 'black',
                    'border-width': 2,
                    'border-opacity': 0.8
                }
            },

            {
                selector: 'edge',
                style: {
                    width: 1,
                    'line-color'(ele) { return relationSettings.getEdgeColor(ele.data().relation) },
                    'target-arrow-color'(ele) { return relationSettings.getEdgeColor(ele.data().relation) },
                    'target-arrow-shape': 'triangle',
                    label(ele) {
                        if (ele.data().labelExt == '') return relationSettings.getEdgeLabel(ele.data().relation)

                        let auxLabelExt = ele.data().labelExt
                        const sigs = ele.data().labelExt.split(',')
                        for (let i = 0; i < sigs.length; i++) {
                            const currentLabel = cy.nodes(`[id='${sigs[i]}']`)[0].data().label + cy.nodes(`[id='${sigs[i]}']`)[0].data().dollar + cy.nodes(`[id='${sigs[i]}']`)[0].data().number
                            auxLabelExt = auxLabelExt.replace(sigs[i], currentLabel)
                        }
                        ele.data().updatedLabelExt = auxLabelExt
                        return `${ele.data().label}[${auxLabelExt}]`

                        ele.data().updatedLabelExt = auxLabelExt
                        return `${ele.data().label}[${auxLabelExt}]`
                    },
                    'curve-style': 'bezier',
                    'text-valign': 'center',
                    'text-outline-color': '#ff3300',
                    'edge-text-rotation': 'autorotate',
                    'line-style'(ele) { return relationSettings.getEdgeStyle(ele.data().relation) },
                    visibility(ele) {
                        const val = relationSettings.isShowAsArcsOn(ele.data().relation)
                        return val ? 'visible' : 'hidden'
                    }
                }
            },
            {
                selector: ':selected',
                style: {
                    'background-opacity': 0.5
                }
            },
            {
                selector: '.multiline-manual',
                style: {
                    'text-wrap': 'wrap'
                }
            },
            {
                selector: 'edge:selected',
                style: {
                    width: 5
                }
            },
            {
                selector: ':parent',
                style: {
                    'background-opacity': 0.3,
                    'text-valign': 'top'
                }
            }

        ],

        layout: {
            name: 'grid',
            fit: true,
            sort(a, b) {
                return a.data('label') < b.data('label')
            },
            avoidOverlap: true
        }

    })

    // right click event on cytoscape's node
    cy.on('cxttap', 'node', {}, (evt) => {
        // close side panel
        $('.settings-panel').removeClass('open')
        // Place right click options menu on mouse position and display it
        $('#optionsMenu').css({
            // overlap cytoscape canvas
            'z-index': 10,
            position: 'absolute',
            // +1 avoids recapturing right click event and opening browser's context menu.
            top: evt.originalEvent.offsetY + 1,
            // if the menu's width overflows page width, place it behind the cursor.
            left: evt.originalEvent.screenX + 1 + 300 > $(window).width() ? evt.originalEvent.offsetX + 1 - 300 : evt.originalEvent.offsetX + 1
        }).fadeIn('slow')
        Session.set('rightClickRel', undefined)
        Session.set('rightClickSig', evt.cyTarget.data().type)
        updateRightClickContent()
        return false
    })

    // right click event on cytoscape's node
    cy.on('cxttap', 'edge', {}, (evt) => {
        // close side panel
        $('.settings-panel').removeClass('open')
        // Place right click options menu on mouse position and display it
        $('#optionsMenu').css({
            // overlap cytoscape canvas
            'z-index': 10,
            position: 'absolute',
            // +1 avoids recapturing right click event and opening browser's context menu.
            top: evt.originalEvent.offsetY + 1,
            // if the menu's width overflows page width, place it behind the cursor.
            left: evt.originalEvent.screenX + 1 + 300 > $(window).width() ? evt.originalEvent.offsetX + 1 - 300 : evt.originalEvent.offsetX + 1
        }).fadeIn('slow')
        Session.set('rightClickSig', undefined)
        Session.set('rightClickRel', evt.cyTarget.data().relation)
        updateRightClickContent()
        return false
    })

    // left click event on cytoscape canvas
    cy.on('tap', (event) => {
        const evtTarget = event.cyTarget

        // If clicked background (not a node or edge)
        if (evtTarget === cy) {
            $('.relation-settings').slideUp()
            $('.atom-settings').slideUp()
            $('.general-settings').slideDown()
        } else {
            // Clicked a node
            if (evtTarget.isNode()) {
                Session.set('selectedSig', evtTarget.data().type)
                $('.general-settings').slideUp()
                $('.relation-settings').slideUp()
                $('.atom-settings').slideDown()
                // Redisplay options hidden by subsetsig selection
                $('.projection-settings').show()
                $('.hide-unconnected-settings').show()
                $('.hide-nodes-settings').show()
                $('.number-nodes-settings').show()
            } else {
                // Clicked an edge
                Session.set('selectedRelation', evtTarget.data().relation)
                $('.general-settings').slideUp()
                $('.atom-settings').slideUp()
                $('.relation-settings').slideDown()
            }
        }
    })

    cy.on('tap', (event) => {
        // hide right click menu
        $('#optionsMenu').hide()
        // close side panel
        $('.settings-panel').removeClass('open')
    })

    cy.on('render', (event) => {
        instChanged()
    })
}
