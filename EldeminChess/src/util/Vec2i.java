package util;

import chess.instance.piece.Piece;

public class Vec2i {
	
	public static interface PosConsumer {
		void run(Vec2i pos);
	}
	
	public int x;
	public int y;
	public Vec2i(int x, int y) {
		super();
		this.x = x;
		this.y = y;
	}
	public <T> Vec2i(T[][] mat) {
		x = mat.length;
		y = mat[0].length;
	}
	
	public static void forEach(int xBound, int yBound, PosConsumer consumer) {
		for (int x = 0; x < xBound; x++) {
			for (int y = 0; y < yBound; y++) {
				consumer.run(new Vec2i(x, y));
			}
		}
	}

	public static void forEach(Object[][] mat, PosConsumer consumer) {
		for (int x = 0; x < mat.length; x++) {
			for (int y = 0; y < mat[0].length; y++) {
				consumer.run(new Vec2i(x, y));
			}
		}
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + x;
		result = prime * result + y;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Vec2i other = (Vec2i) obj;
		if (x != other.x)
			return false;
		if (y != other.y)
			return false;
		return true;
	}
	public Vec2i add(Vec2i vec2i) {
		return new Vec2i(x + vec2i.x, y + vec2i.y);
	}
	public Vec2i subtract(Vec2i vec2i) {
		return new Vec2i(x - vec2i.x, y - vec2i.y);
	}
	public void forEach(PosConsumer consumer) {
		for (int i = 0; i < x; i++) {
			for (int j = 0; j < y; j++) {
				consumer.run(new Vec2i(i, j));
			}
		}
	}
	public <T> T in(T[][] mat) {
		return mat[x][y];
	}
	public <T> void setIn(T[][] mat, T value) {
		mat[x][y] = value;
	}
}
