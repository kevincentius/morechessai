package chess.instance.piece;

public class Piece {

	private int id;
	private int team;
	
	public Piece(int pieceRuleId, int team) {
		super();
		this.id = pieceRuleId;
		this.team = team;
	}
	public int getTeam() {
		return team;
	}
	public int getId() {
		return id;
	}
	
	public Piece copy() {
		return new Piece(id, team);
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + team;
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
		Piece other = (Piece) obj;
		if (id != other.id)
			return false;
		if (team != other.team)
			return false;
		return true;
	}
	
}
