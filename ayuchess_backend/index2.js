    const { Game } = require('js-chess-engine');
    const { Chess } = require('chess.js')
            
    const FEN = "1k3br1/prq1p1pp/2p1b3/1p1pPp2/3Q4/2N1PP1P/PPPRB1P1/1K5R w - - 3 19"
    const chess = new Chess(FEN)
    const g = new Game(FEN)
    const z = g.aiMove()
    const move = Object.keys(z).toString().toLowerCase() + z[Object.keys(z)[0]].toLowerCase()
    chess.move(move)
    console.log(chess.fen())



    const zz = {"uid": "bar12", "gameId": "pending"}