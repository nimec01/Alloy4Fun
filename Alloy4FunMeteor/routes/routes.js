/**
 * Created by josep on 07/02/2016.
 */

//Default page behaviours
Router.configure({
    //Template displayed while loading data.
    loadingTemplate: 'loading',
    //Template displayed when there's no route for the sub domain.
    notFoundTemplate: 'notFound',
    //Main page template, useful to display copyrights and menus in every page.
    layoutTemplate: 'layout'
});


//Home page routing
Router.route('/', {
    //Name of the template to render. Can ve invoked using Router.go('templateName');
    name: 'home',
    //Meteor template to render
    template: 'home',
    //Visit the documentation for more info on "where"
    where: 'client'
});

Router.route('/editor', {
    name: 'editor',
    template: 'alloyEditor',
    where: 'client'
});

Router.route('/editor/:_id', {
    name: 'editorLoad',
    template: 'alloyEditor',
    //Check routes/controllers/editorLoadController for more details
    controller :"editorLoadController",
    where: 'client'
});

//route para editar challenge já criado [erro]
Router.route('/createChallenge/:_id',{
    name: 'editChallenge',
    template: 'createChallengeWrapper',
    where: 'client',
    controller: 'editChallengeController'
});

Router.route('/createChallenge',{
    name: 'createChallenge',
    template: 'createChallengeWrapper',
    where: 'client'
});

Router.route('/solveChallenge/:_id',{
    name: 'solveChallenge',
    template: 'solveChallenge',
    controller :"solveChallengeController",
    where: 'client'
});