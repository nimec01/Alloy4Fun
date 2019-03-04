import {
    displayError
} from "../editor/feedback"

// the list of types currently projected
currentlyProjectedTypes = [];
// for each of the types, the selected frame
currentFramePosition = {};
// all cy nodes available in the unprojected instance
allNodes = [];
// stores the positions of the nodes between frames
nodePositions = {};

// will call the projection API for the current projections/frames
project = function() {
    Meteor.call("getProjection", getCurrentInstance().sessionId, currentFramePosition, instanceIndex, processProjection);
};

// processes a frame for projected instance from API response
processProjection = function(err, projection) {
    if (err) return displayError(err)
    frame = projection[0];
    // process atoms and subsets
    cy.nodes().remove();
    allNodes.forEach((node) => {
        for (const i in frame.atoms) {
            if (node.data().id == frame.atoms[i]) {
                // for each atom, check relations on frame's atom_rels
                for (let ar = 0; ar < frame.atom_rels.length; ar++) {
                    if (frame.atom_rels[ar].atom == node.data().id) {
                        // the atom has relations
                        // create the array, or replace by empty
                        node.data().subsetSigs = [];
                        // add relations to subset sigs
                        for (let r = 0; r < frame.atom_rels[ar].relations.length; r++) {
                            node.data().subsetSigs.push(frame.atom_rels[ar].relations[r]);
                        }
                        break;
                    }
                }
                // add nodes that are present in frame
                cy.add(node);
            }
        }
    });
    // process relations
    cy.edges().remove();
    const edges = getProjectionEdges(frame.relations);
    cy.add(edges);
    // apply the layout (being applied twice)
    applyCurrentLayout();
    // recover node positions
    applyPositions();
};

getProjectionEdges = function(relations) {
    const result = [];
    relations.forEach((relation) => {
        if (relation.relation != 'Next' && relation.relation != 'First') {
            for (let i = 0; i < relation.tuples.length; i += relation.arity) {
                let tuple = [];
                for (let j = i; j < relation.arity + i; j++) tuple.push(relation.tuples[j]);
                const tempTuple = tuple.slice(0);
                const labelExt = tuple.splice(1, tuple.length - 2).toString();
                tuple = tempTuple;
                result.push({
                    group: 'edges',
                    selectable: true,
                    data: {
                        relation: relation.relation,
                        source: tuple[0],
                        target: tuple[tuple.length - 1],
                        label: getRelationLabel(relation.relation),
                        color: getRelationColor(relation.relation),
                        labelExt,
                        updatedLabelExt: labelExt,
                        edgeStyle: getRelationEdgeStyle(relation.relation),
                    },
                });
            }
        }
    });
    return result;
};

// projects a new signature, updates elements accordingly
addTypeToProjection = function(newType) {
    const atoms = lastFrame(newType);
    if (currentlyProjectedTypes.indexOf(newType) == -1) {
        currentlyProjectedTypes.push(newType);
        currentlyProjectedTypes.sort();
        $('.frame-navigation').show();
        $('.frame-navigation > select').append($('<option></option>')
            .attr('value', newType)
            .text(newType));
        if (atoms >= 0) 
            currentFramePosition[newType] = 0;
    } else throw `${newType} already being projected.`;
    if (atoms >= 1)
        $('#nextFrame').addClass('enabled');
    else
        $('#nextFrame').removeClass('enabled');
    $('#previousFrame').removeClass('enabled');
    $('.current-frame').html(currentFramePositionToString());
    $('.framePickerTarget').val(newType);
    project();
};

// removes a projected signature, updates elements accordingly
removeTypeFromProjection = function(type) {
    const index = currentlyProjectedTypes.indexOf(type);
    if (index == -1) throw `${type} not found in types being projected.`;
    else {
        currentlyProjectedTypes.splice(index, 1);
        delete currentFramePosition[type];
        $(`.frame-navigation > select option[value = '${type}']`).remove();
    }
    if (currentlyProjectedTypes.length == 0) {
        $('.frame-navigation').hide();
        const instance = getCurrentInstance();
        if (instance) updateGraph(instance);
    } else {
        $('.current-frame').html(currentFramePositionToString());
        project();
    }
};

// applies the current projected information to a new instance, same projected
// signatures but resets frame selection; elements updated accordingly
newInstanceSetup = function() {
    currentFramePosition = {};
    if (currentlyProjectedTypes.length != 0) {
        for (const key in currentlyProjectedTypes) 
            currentFramePosition[currentlyProjectedTypes[key]] = 0;
        $('.current-frame').html(currentFramePositionToString());
        allNodes = cy.nodes();
        project();
        const atoms = lastFrame($('.framePickerTarget')[0].value);
        if (atoms >= 1) 
            $('#nextFrame').addClass('enabled');
        else 
            $('#nextFrame').removeClass('enabled');
        $('#previousFrame').removeClass('enabled');
        $('.frame-navigation > select').prop('disabled',false);
    } else {
        $(".frame-navigation").hide();
    }
};

// updates the frame navigator according to a static instance (i.e., 
// everything disabled)
staticProjection = function() {
    console.log(currentlyProjectedTypes)
    console.log(currentFramePosition)
    $('.frame-navigation > select').append($('<option></option>')
        .attr('value', currentlyProjectedTypes[0])
        .text(currentlyProjectedTypes[0]));
    $('.current-frame').html(currentFramePositionToString());
    $('#nextFrame').removeClass('enabled');
    $('#previousFrame').removeClass('enabled');
    $('.frame-navigation').show();
    $('.frame-navigation > select').prop('disabled',true);
};

// saves current node positions
savePositions = function() {
    const atoms = cy.nodes();
    atoms.forEach((atom) => {
        nodePositions[atom.data().id] = jQuery.extend(true, {}, atom.position());
    });
};

// applies saved node positions
applyPositions = function() {
    for (const id in nodePositions) {
        const node = cy.nodes(`[id='${id}']`);
        if (node.length > 0) {
            node[0].position(nodePositions[id]);
        }
    }
};

// resets saved node positions
resetPositions = function() {
    nodePositions = {};
};

// calculates the label to be present as the current frame, type+index
currentFramePositionToString = function() {
    const position = [];
    for (const key in currentFramePosition) position.push(key + currentFramePosition[key]);
    return position.toString();
};

/*
 TODO caching system
 getProjectionFromCache = function (typesToProject){
 for(var i in projectionCache)
 if(projectionCache[i].projectedTypes.equals(typesToProject))return projectionCache[i].frames;
 return undefined;
 };

 cacheProjectionState = function(){

 };

 isProjectionCached = function (typesToProject){
 for(var i in projectionCache)
 if(projectionCache[i].projectedTypes.equals(typesToProject))return true;
 return false;
 }; */

