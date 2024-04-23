const functions = require('firebase-functions');
const admin = require('firebase-admin');
const { Chess } = require('chess.js');
const { Game } = require('js-chess-engine');
const { v4: uuidv4 } = require('uuid');

admin.initializeApp();


exports.matchPlayers = functions.database.ref('/lobby/{userId}')
    .onWrite(async (change, context) => {
        const lobbyRef = admin.database().ref('/lobby');
        const snapshot = await lobbyRef.once('value');
        const lobby = snapshot.val();
        console.log(lobby)

        const pendingPlayers = [];
        Object.keys(lobby).forEach(uid => {
            if (lobby[uid].gameId === 'pending') {
                pendingPlayers.push(uid);
            }
        });
        console.log(pendingPlayers)

        if (pendingPlayers.length >= 2) {
            const newGameId = uuidv4();

            const updates = {};
            updates[pendingPlayers[0] + '/gameId'] = newGameId;
            updates[pendingPlayers[1] + '/gameId'] = newGameId;

            if (Math.random() > 0.5) {
                updates[pendingPlayers[0] + '/side'] = "white";
                updates[pendingPlayers[1] + '/side'] = "black";
            }
            else {
                updates[pendingPlayers[0] + '/side'] = "black";
                updates[pendingPlayers[1] + '/side'] = "white";
            }
            console.log(updates)
            await lobbyRef.update(updates);
        }

        return null;
    });


exports.updateFEN = functions.database.ref('/games/{gameId}/turn')
    .onUpdate(async (change, context) => {
        const gameId = context.params.gameId;
        const gameRef = admin.database().ref(`/games/${gameId}`);
        const gameSnapshot = await gameRef.once('value');
        const gameData = gameSnapshot.val();

        if (gameData.turn !== "ai") {
            console.log("Not AI's turn.");
            return null;
        }

        const delayRandom = () => {
            const minDelay = 1000;
            const maxDelay = 3000;
            const delay = Math.random() * (maxDelay - minDelay) + minDelay;
        
            return new Promise(resolve => setTimeout(resolve, delay));
        }
        
        await delayRandom()
        const newFen = gameData.fen;
        const chess = new Chess(newFen);
        const game = new Game(newFen);

        const aiMove = game.aiMove(0);
        const move = Object.keys(aiMove).toString().toLowerCase() + aiMove[Object.keys(aiMove)[0]].toLowerCase();

        chess.move(move);
        const updatedFen = chess.fen();

        await gameRef.update({
            fen: updatedFen,
            turn: "player"
        });

        return null;
    });

