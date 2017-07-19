package chess.rule.piece;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import chess.instance.board.GameState;
import chess.instance.piece.Piece;
import chess.rule.piece.SlideRule.SlideRuleData;
import chess.rule.piece.move.Move;
import chess.rule.piece.move.MoveImpl;
import chess.rule.set.RuleSet;
import javafx.scene.image.Image;
import util.ImageStore;
import util.Vec2i;

public class PieceRuleImpl implements PieceRule {
	
	private class VisiblePieceList {
		public List<Integer> distances;
		public int blockingDistance;
		public VisiblePieceList(List<Integer> distances, int blockingDistance) {
			super();
			this.distances = distances;
			this.blockingDistance = blockingDistance;
		}
	}
	
	private PieceRuleData pieceRuleData;

	private int[][][] slideDirs; // [team][diretion][{xy}]
	private SlideRule[] basicSlideRules;
	private SlideRule[] captureSlideRules;
	private SlideRule[] swapSlideRules;
	
	// array[team][i]
	private Vec2i[][] jumpBasicMoves;
	private Vec2i[][] jumpCaptureMoves;
	private Vec2i[][] jumpSwapMoves;
	
	private Image[] images;
	
	public PieceRuleImpl(PieceRuleData pieceRuleData) {
		this.pieceRuleData = pieceRuleData;
		
		slideDirs = getSlideDirs(pieceRuleData);
		if (slideDirs != null) {
			basicSlideRules = getSlideRuleArray(pieceRuleData.slideMove, slideDirs[0]);
			captureSlideRules = getSlideRuleArray(pieceRuleData.slideCapture, slideDirs[0]);
			swapSlideRules = getSlideRuleArray(pieceRuleData.slideSwap, slideDirs[0]);
		}
		
		jumpBasicMoves = getTransposedSparseBooleanMatrix(pieceRuleData.jumpMove);
		jumpCaptureMoves = getTransposedSparseBooleanMatrix(pieceRuleData.jumpCapture);
		jumpSwapMoves = getTransposedSparseBooleanMatrix(pieceRuleData.jumpSwap);
		
		images = ImageStore.getInstance().getPieceImages(pieceRuleData.image);
	}

	private SlideRule[] getSlideRuleArray(SlideRuleData[] slideMove, int[][] dirs) {
		if (slideMove == null) {
			return null;
		}
		
		SlideRule[] res = new SlideRule[dirs.length];
		for (int dirId = 0; dirId < dirs.length; dirId++) {
			for (SlideRuleData data : slideMove) {
				int matchDirId = -1;
				for (int dataDirId = 0; dataDirId < data.dirs.length; dataDirId++) {
					if (Arrays.equals(dirs[dirId], data.dirs[dataDirId])) {
						matchDirId = dataDirId;
						break;
					}
				}
				
				if (matchDirId != -1) {
					res[dirId] = new SlideRule(data.minDist, data.maxDist, data.distList);
					break;
				}
			}
		}
		return res;
	}

	private int[][][] getSlideDirs(PieceRuleData pieceRuleData) {
		HashSet<Vec2i> set = new HashSet<>(); 
		SlideRuleData[][] slides = new SlideRuleData[][]{ pieceRuleData.slideMove, pieceRuleData.slideCapture, pieceRuleData.slideSwap };
		for (SlideRuleData[] slide : slides) {
			if (slide != null) {
				for (SlideRuleData slideRule : slide) {
					for (int[] dir : slideRule.dirs) {
						set.add(new Vec2i(dir[0], dir[1]));
					}
				}				
			}
		}
		if (set.isEmpty()) {
			return null;
		} else {
			int[][] slideDirs = new int[set.size()][];
			int[][] slideDirsReversed = new int[set.size()][];
			int i = 0;
			for (Vec2i dir : set) {
				slideDirs[i] = new int[]{ dir.x, dir.y };
				slideDirsReversed[i] = new int[]{ dir.x, -dir.y };
				i++;
			}
			return new int[][][]{ slideDirs, slideDirsReversed };			
		}
	}

	private Vec2i[][] getTransposedSparseBooleanMatrix(Boolean[][] mat) {
		List<Vec2i> sparseMat = new ArrayList<>(); // for black
		List<Vec2i> sparseMatReversed = new ArrayList<>(); // for white
		if (mat != null) {
			Vec2i offset = new Vec2i(mat.length / 2, mat[0].length / 2);
			Vec2i.forEach(mat, pos -> {
				if (pos.in(mat)) {
					Vec2i relPos = pos.subtract(offset);
					sparseMat.add(new Vec2i(relPos.y, relPos.x));
					sparseMatReversed.add(new Vec2i(relPos.y, -relPos.x));
				}
			});
		}
		return new Vec2i[][]{ sparseMat.toArray(new Vec2i[0]), sparseMatReversed.toArray(new Vec2i[0]) };
	}

	@Override
	public Image getImage(int team) {
		return images[team];
	}

	@Override
	public boolean isBlocking() {
		return pieceRuleData.blocking;
	}

	@Override
	public int[] getPromotionList() {
		return pieceRuleData.promotionList;
	}
	
	@Override
	public List<Move> getAllLegalMoves(GameState state, RuleSet ruleSet, Vec2i position) {
		List<Move> legalMoves = new ArrayList<>();
		addSlideMoves(legalMoves, state, ruleSet, position);
		addJumpMoves(state, ruleSet, position, legalMoves);
		return legalMoves;
	}

	private void addJumpMoves(GameState state, RuleSet ruleSet, Vec2i position, List<Move> legalMoves) {
		PieceRule pieceRule = ruleSet.getPieceRule(state.getPiece(position).getId());
		
		addJumpBasicMoves(state, pieceRule, position, legalMoves);
		addJumpCaptureMoves(state, pieceRule, position, legalMoves);
		addJumpSwapMoves(state, pieceRule, position, legalMoves);
	}

	private void addJumpBasicMoves(GameState state, PieceRule pieceRule, Vec2i position, List<Move> legalMoves) {
		for (Vec2i relPos : jumpBasicMoves[state.getTurn()]) {
			Vec2i target = position.add(relPos);
			// Potential optimization: pre-determine which relPos are inside the board for every possible case/position
			if (state.isInside(target)) {
				if (state.getPiece(target) == null) {
					addMovesWithPromotionCheck(state, position, target, false, legalMoves);
				}
			}
		}
	}

	private void addJumpCaptureMoves(GameState state, PieceRule pieceRule, Vec2i position, List<Move> legalMoves) {
		for (Vec2i relPos : jumpCaptureMoves[state.getTurn()]) {
			Vec2i target = position.add(relPos);
			// Potential optimization: pre-determine which relPos are inside the board for every possible case/position
			if (state.isInside(target)) {
				Piece piece = state.getPiece(target);
				if (piece != null && piece.getTeam() != state.getTurn()) {
					addMovesWithPromotionCheck(state, position, target, false, legalMoves);
				}
			}
		}
	}

	private void addJumpSwapMoves(GameState state, PieceRule pieceRule, Vec2i position, List<Move> legalMoves) {
		for (Vec2i relPos : jumpSwapMoves[state.getTurn()]) {
			Vec2i target = position.add(relPos);
			// Potential optimization: pre-determine which relPos are inside the board for every possible case/position
			if (state.isInside(target)) {
				Piece piece = state.getPiece(target);
				if (piece != null && piece.getTeam() == state.getTurn()) {
					addMovesWithPromotionCheck(state, position, target, true, legalMoves);
				}
			}
		}
	}

	private void addMovesWithPromotionCheck(GameState state, Vec2i position, Vec2i target, boolean swap, List<Move> legalMoves) {
		// Potential optimization: pieceRule.getPromotionList() != null is evaluated everytime. Something can be done to reduce this.
		if (pieceRuleData.promotionList != null && state.isPromotingSquare(target, state.getTurn())) {
			for (int promoteTo : pieceRuleData.promotionList) {
				legalMoves.add(new MoveImpl(position, target, swap, promoteTo));
			}
		} else {
			legalMoves.add(new MoveImpl(position, target, swap, -1));
		}
	}

	private void addSlideMoves(List<Move> legalMoves, GameState state, RuleSet ruleSet, Vec2i position) {
		if (slideDirs == null) {
			return;
		}
		
		
		for (int direction = 0; direction < slideDirs[0].length; direction++) {
			VisiblePieceList visiblePieceList = findVisiblePieceDistances(state, ruleSet, position, direction);
			
			addSlideBasicMoves(legalMoves, state, position, direction, visiblePieceList);
			if (!visiblePieceList.distances.isEmpty()) {
				addSlideCaptureMoves(legalMoves, state, position, direction, visiblePieceList);
				addSlideSwapMoves(legalMoves, state, position, direction, visiblePieceList);				
			}
		}
	}

	private void addSlideBasicMoves(List<Move> legalMoves, GameState state, Vec2i position, int direction,
			VisiblePieceList visiblePieceList) {
		if (basicSlideRules != null && basicSlideRules[direction] != null) {
			SlideRule slideRule = basicSlideRules[direction];
			
			int maxDist = Math.min(slideRule.maxDist, visiblePieceList.blockingDistance - 1);
			
			if (maxDist >= slideRule.minDist) {
				for (int distance = slideRule.minDist; distance <= maxDist; distance++) {
					Vec2i target = slide(position, slideDirs[state.getTurn()][direction], distance);
					//Potential optimization: calculate board limit in maxDist instead of using isInside(...) using function table for the 8 directions 
					if (!state.isInside(target)) {
						break;
					} else if (state.getPiece(target) == null) {
						if (distance < maxDist) {
							legalMoves.add(new MoveImpl(position, target, false, -1));
						} else {
							addMovesWithPromotionCheck(state, position, target, false, legalMoves);
						}
					}
				}
			}
			
			// TODO: implement for slideRule.distList
		}
	}

	private void addSlideCaptureMoves(List<Move> legalMoves, GameState state, Vec2i position, int direction, VisiblePieceList visiblePieceList) {
		if (captureSlideRules != null && captureSlideRules[direction] != null) {
			SlideRule slideRule = captureSlideRules[direction];
			int turn = state.getTurn();
			
			if (slideRule.maxDist != 0) {
				int[] distIdRange = getDistIdRange(visiblePieceList.distances, slideRule);
				
				for (int distId = distIdRange[0]; distId <= distIdRange[1]; distId++) {
					Vec2i target = slide(position, slideDirs[state.getTurn()][direction], visiblePieceList.distances.get(distId));
					if (state.getPiece(target).getTeam() != turn) {
						if (distId < distIdRange[1]) {
							legalMoves.add(new MoveImpl(position, target, false, -1));
						} else {
							addMovesWithPromotionCheck(state, position, target, false, legalMoves);
						}
					}
				}
			}
			
			// TODO: implement for slideRule.distList
		}
	}

	private void addSlideSwapMoves(List<Move> legalMoves, GameState state, Vec2i position, int direction, VisiblePieceList visiblePieceList) {
		if (swapSlideRules != null && swapSlideRules[direction] != null) {
			SlideRule slideRule = swapSlideRules[direction];
			int turn = state.getTurn();
			
			if (slideRule.maxDist != 0) {
				int[] distIdRange = getDistIdRange(visiblePieceList.distances, slideRule);

				for (int distId = distIdRange[0]; distId <= distIdRange[1]; distId++) {
					Vec2i target = slide(position, slideDirs[state.getTurn()][direction], visiblePieceList.distances.get(distId));
					if (state.getPiece(target).getTeam() == turn) {
						if (distId < distIdRange[1]) {
							legalMoves.add(new MoveImpl(position, target, true, -1));
						} else {
							addMovesWithPromotionCheck(state, position, target, true, legalMoves);
						}
					}
				}
			}
			// TODO: implement for slideRule.distList
		}
	}

	/**
	 * returns the start and end (both inclusive!) index in pieceDistances that are within the slide range defined by slideRule 
	 * @param pieceDistances result of findVisiblePieceDistances(...)
	 * @param slideRule from the piece rule - defines the min and max slide distance
	 * @return
	 */
	private int[] getDistIdRange(List<Integer> pieceDistances, SlideRule slideRule) {
		int[] distIdRange = new int[2];
		for (distIdRange[0] = 0; distIdRange[0] < pieceDistances.size(); distIdRange[0]++) {
			if (pieceDistances.get(distIdRange[0]) >= slideRule.minDist) {
				break;
			}
		}

		for (distIdRange[1] = pieceDistances.size() - 1; distIdRange[1] >= distIdRange[0]; distIdRange[1]--) {
			if (pieceDistances.get(distIdRange[1]) <= slideRule.maxDist) {
				break;
			}
		}
		return distIdRange;
	}

	/**
	 * returns the resulting position after sliding from the given position
	 * @param position starting position of the sliding move
	 * @param direction direction of slide (8 directions)
	 * @param distance distance of slide
	 * @return
	 */
	private Vec2i slide(Vec2i position, int[] dir, int distance) {
		int x = position.x + distance * dir[0];
		int y = position.y + distance * dir[1];
		Vec2i target = new Vec2i(x, y);
		return target;
	}

	/**
	 * Finds all pieces that can be 'seen' from `position` along `direction`.
	 * Pieces can still be seen if it is behind a non-blocking piece.
	 * Meant for sliding move generation.
	 * @param state
	 * @param ruleSet
	 * @param position
	 * @param direction
	 * @return
	 */
	private VisiblePieceList findVisiblePieceDistances(GameState state, RuleSet ruleSet, Vec2i position, int direction) {
		// TODO: optimization: include the board-edge limit in the result so the slideBasicMove generation can be further optimized
		// TODO: optimization: pre-calculate ranges
		int[] range = new int[]{ Integer.MAX_VALUE, Integer.MIN_VALUE };
		extendRange(range, basicSlideRules, direction);
		extendRange(range, captureSlideRules, direction);
		extendRange(range, swapSlideRules, direction);
		if (range[0] > range[1]) {
			
		}
		
		int blockingDistance = Integer.MAX_VALUE;
		List<Integer> distances = new ArrayList<Integer>(); // distances at which squares are occupied by pieces
		for (int distance = range[0]; distance <= range[1]; distance++) {
			int x = position.x + distance * slideDirs[state.getTurn()][direction][0];
			int y = position.y + distance * slideDirs[state.getTurn()][direction][1];
			// Potential optimization: use function table to find board-edge limit instead of isInside(...)
			if (state.isInside(x, y)) {
				Piece piece = state.getPiece(x, y);
				if (piece != null) {
					distances.add(distance);
					if (ruleSet.getPieceRule(piece.getId()).isBlocking()) {
						blockingDistance = distance;
						break;
					}
				}
			} else {
				break;
			}
		}
		return new VisiblePieceList(distances, blockingDistance);
	}

	private void extendRange(int[] range, SlideRule[] slideRules, int direction) {
		if (slideRules != null && slideRules[direction] != null) {
			SlideRule slideRule = slideRules[direction];
			range[0] = Math.min(range[0], slideRule.minDist);
			range[1] = Math.max(range[1], slideRule.maxDist);
		}
	}

	@Override
	public int[] getDeathTriggers() {
		return pieceRuleData.deathTriggers;
	}
	
}
