package chess.instance.board;

import java.nio.ByteBuffer;

import chess.instance.piece.Piece;
import util.MurmurHash3;
import util.Vec2i;
import util.MurmurHash3.LongPair;

public class GameState {
	
	private int hashSeed = 489765132;
	
	private Piece[][] pieceMat;
	private int turn;
	private int winner = -1;
	
	// ---- Redundant information - not included in hash ----
	private int materialScore = 0;
	// ---- End of : Redundant information ------------------
	
	public GameState(Vec2i size) {
		pieceMat = new Piece[size.x][size.y];
	}
	
	public Vec2i getSize() {
		return new Vec2i(pieceMat.length, pieceMat[0].length);
	}

	public Piece getPiece(Vec2i pos) {
		return pieceMat[pos.x][pos.y];
	}
	public Piece getPiece(int x, int y) {
		return pieceMat[x][y];
	}
	public void setPiece(Vec2i pos, Piece piece) {
		pieceMat[pos.x][pos.y] = piece;
	}
	public int getTurn() {
		return turn;
	}
	public void setTurn(int turn) {
		this.turn = turn;
	}
	public void nextTurn() {
		turn = 1 - turn;
	}
	public int getWinner() {
		return winner;
	}
	public void setWinner(int winner) {
		this.winner = winner;
	}
	public int getMaterialScore() {
		return materialScore;
	}
	public void setMaterialScore(int materialScore) {
		this.materialScore = materialScore;
	}

	public boolean isInside(Vec2i pos) {
		return pos.x >= 0
				&& pos.y >= 0
				&& pos.x < pieceMat.length
				&& pos.y < pieceMat[0].length;
	}

	public boolean isInside(int x, int y) {
		return x >= 0
				&& y >= 0
				&& x < pieceMat.length
				&& y < pieceMat[0].length;
	}
	
	public GameState copy() {
		Vec2i size = new Vec2i(pieceMat);
		
		GameState copy = new GameState(size);
		copy.turn = turn;
		copy.winner = winner;
		
		size.forEach(pos -> copy.setPiece(pos, getPiece(pos)));
		return copy;
	}

	public LongPair get128Hash() {
		byte[] key = new byte[8 + pieceMat.length * pieceMat[0].length * 4];
		ByteBuffer buffer = ByteBuffer.wrap(key);

		buffer.putInt(turn);
		buffer.putInt(winner);
		for (int i = 0; i < pieceMat.length; i++) {
			for (int j = 0; j < pieceMat[i].length; j++) {
				buffer.putInt(pieceMat[i][j] == null ? 0 : (pieceMat[i][j].getId() + 1) * (1 - pieceMat[i][j].getTeam() * 2));
			}
		}
		
		LongPair lp = new LongPair();
		MurmurHash3.murmurhash3_x64_128(key, 0, key.length, hashSeed, lp);
		return lp;
	}

	public boolean isPromotingSquare(Vec2i pos, int team) {
		return pos.y == team * (pieceMat[0].length - 1);
	}

}
