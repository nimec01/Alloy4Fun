import cytoscape from 'cytoscape'
import { updateRightClickContent } from '../../templates/visSettings/rightClickMenu'
import { instChanged } from '../editor/state'
import { newInstanceSetup } from './projection'

updateGraph = function (instance,v) {
    // Remove previous nodes and edges.
    cy.remove(cy.elements())
    // Add new ones.
    const atomElements = getAtoms(instance) 
    cy.add(atomElements)
    cy.add(getEdges(instance))
    cy.resize()
    // Draw data according to the selected layout.
    if (!v) applyCurrentLayout()
}

// Get atom information received from server ready to upload to cytoscape object.
getAtoms = function (instance) {
    const atoms = []
    generalSettings.resetHierarchy()
    instance.types.forEach((sig) => {
        // built-in sigs
        if (sig.name == 'String' || sig.name == 'Int') {
            const tp = sig.name
            generalSettings.addPrimSig(tp, sig.parent)
            sig.atoms.forEach((atom) => {
                atoms.push(
                    {
                        group: 'nodes',
                        classes: 'multiline-manual',
                        data: {
                            id: atom,
                            type: tp,
                            subsetSigs: []
                        }
                    }
                )
            })
        // module sigs
        } else {
            const tp = sig.name.indexOf('/') > -1 ? sig.name.split('/')[1] : sig.name
            const pa = sig.parent.indexOf('/') > -1 ? sig.parent.split('/')[1] : sig.parent
            generalSettings.addPrimSig(tp, pa)
            sig.atoms.forEach((atom) => {
                atoms.push(
                    {
                        group: 'nodes',
                        classes: 'multiline-manual',
                        data: {
                            id: atom,
                            type: tp,
                            subsetSigs: []
                        }
                    }
                )
            })
        }
    })

    instance.sets.forEach((set) => {
        set.atoms.forEach((atom) => {
            const tp = set.name.indexOf('/') > -1 ? set.name.split('/')[1] : set.name
            for (let i = 0; i < atoms.length; i++) {
                if (atoms[i].data.id == atom) {
                    let paren = atoms[i].data.type
                    paren = paren.indexOf('/') > -1 ? paren.split('/')[1] : paren
                    const canon = `${tp}:${paren}`
                    if (!generalSettings.hasSubsetSig(canon)) {
                        generalSettings.addSubSig(canon, paren)
                    }
                atoms[i].data.subsetSigs.push(canon)
                }
            }
        })
    })

    return atoms
}

getEdges = function (instance) {
    const result = []
    instance.rels.forEach((field) => {
        field.atoms.forEach((relation) => {
            result.push({
                group: 'edges',
                selectable: true,
                data: {
                    relation: field.name,
                    source: relation[0],
                    target: relation[relation.length - 1],
                    atoms: relation
                }
            })
        })
    })
    return result
}

// Selecting an element on the cytoscape canvas recalculates its label. Useful after editing labels
refreshGraph = function () {
    const selected = cy.$(':selected')
    cy.elements().select().unselect()
    selected.select()
}

initGraphViewer = function (element) {
    // the element must be visible for the layout to be correctly 
    // applied, but the template will only trigger after
    document.getElementById(element).parentElement.parentElement.removeAttribute('hidden')
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
                        let l = relationSettings.calculateNodeLabel(ele)
                    
                        // subsig labels
                        const subsigs = ele.data().subsetSigs.length > 0 ? `\n(${ele.data().subsetSigs.map(x => x.split(':')[0])})` : ''

                        // relations as attributes labels
                        let attributes = relationSettings.getAttributeLabel(ele)

                        return `${l}${subsigs}\n${attributes}`
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
                        let val1 = true
                        if (ele.data().subsetSigs.length > 0) {
                            // only hide if all subsigs want to hide
                            ele.data().subsetSigs.forEach(ss =>
                                val1 = val1 && sigSettings.getInheritedAtomVisibility(ss))
                        } else {
                            val1 = sigSettings.getInheritedAtomVisibility(ele.data().type)
                        }
                        return val1 ? 'hidden' : 'visible'
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
                    'line-color'(ele) { 
                        return relationSettings.getEdgeColor(ele.data().relation) 
                    },
                    'target-arrow-color'(ele) { 
                        return relationSettings.getEdgeColor(ele.data().relation) 
                    },
                    'target-arrow-shape': 'triangle',
                    'label'(ele) {
                        return relationSettings.getEdgeLabel(ele)
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

    cy.on('cxttap', function(evt){
      var evtTarget = evt.cyTarget;    
      if( evtTarget === cy ){
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
        Session.set('rightClickSig', undefined)
        updateRightClickContent()
        return false
      }
    });

    // right click event on cytoscape's node
    cy.on('cxttap', 'node', {}, (evt) => {
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
        Session.set('rightClickSig', [evt.cyTarget.data().type].concat(evt.cyTarget.data().subsetSigs))
        updateRightClickContent()
        return false
    })

    // right click event on cytoscape's node
    cy.on('cxttap', 'edge', {}, (evt) => {
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
        Session.set('rightClickRel', [evt.cyTarget.data().relation])
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
                $('.hide-nodes-settings').show()
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
    })

    cy.on('render', (event) => {
        instChanged()
    })
}
