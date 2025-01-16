package TicTacToe;

import GameInterfaces.Move;

import java.io.Serializable;

public class TTT_Move implements Move, Serializable {
    private int player;
    private int row;
    private int col;

    public TTT_Move(int player, int row, int col) {
        this.player = player;
        this.row = row;
        this.col = col;
    }

    @Override
    public int getPlayer() {return player;}

    @Override
    public int getRow() {return row;}

    @Override
    public int getColumn() {return col;}
}
