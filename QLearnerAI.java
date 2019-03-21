import java.lang.reflect.Array;
import java.util.*;
import java.io.*;


public class QLearnerAI extends AIModule{

    private static double gamma = 0.99;
    public static HashMap<String, int[]> state_action_count = new HashMap<>();
    public static HashMap<String, String[]> state_action_values = new HashMap<>();
    public static HashMap<String, String[]> state_action_seed_check = new HashMap<>();

    int is_training;

    public QLearnerAI(int is_training){
        this.is_training = is_training;
    }

    class Board{
        String state;
        ArrayList<Integer> legalActions;
        String[] q_values;
        public Board(ArrayList<Integer> legalActions, String state, String[] q_values){
            this.legalActions = legalActions;
            this.state = state;
            this.q_values = q_values;
        }
    }


    @Override
    public void getNextMove(GameStateModule game) {
        if (is_training == 1){
            Board curr_board = getStateActionValues(game);
            chosenMove = selectMove(curr_board.legalActions, curr_board.q_values);
            int chosenMoveCopy = chosenMove;
            updateQTable(game, curr_board, chosenMoveCopy);
        }else{
            try{
                Board curr_board = getStateActionValuesFromFile(game);
                chosenMove = selectMove(curr_board.legalActions, curr_board.q_values);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }


    private Board getStateActionValuesFromFile(GameStateModule game) throws Exception{
        String currState = "";
        int nonzeros = 0;
        ArrayList<Integer> legalActions = new ArrayList<>();
        for(int i=0;i<game.getWidth();i++) {
            // legal actions
            if (game.canMakeMove(i)) {
                legalActions.add(i);
            }
            // current state
            for (int j = 0; j < game.getHeight(); j++) {
                currState += String.valueOf(game.getAt(i, j));
                if (game.getAt(i, j) != 0)
                    nonzeros += 1;
            }
        }
        File f = new File("qtables/" + nonzeros + ".txt");
        BufferedReader br = new BufferedReader(new FileReader(f));
        String[] q_values = new String[game.getWidth()];
        for (int i=0;i<q_values.length;i++){
            q_values[i] = "0";
        }
        String line;
        while ((line = br.readLine())!=null){
            line = line.substring(0, line.length()-1);
            String[] spl = line.split(":");
            if (spl[0].equals(currState)) {
                q_values = spl[1].split(" ");
                break;
            }
        }
        return new Board(legalActions, currState, q_values);
    }


    private Board getStateActionValues(GameStateModule game){
        String currState = "";
        ArrayList<Integer> legalActions = new ArrayList<>();
        for(int i=0;i<game.getWidth();i++){

            if (game.canMakeMove(i)){
                legalActions.add(i);
            }

            for(int j=0;j<game.getHeight();j++){
                currState += String.valueOf(game.getAt(i, j));
            }
        }

        String[] q_values = state_action_values.get(currState);
        if (q_values == null){
            String[] action_values = new String[game.getWidth()];
            for (int i = 0; i < game.getWidth(); i++)
                action_values[i] = "0";
            q_values = action_values;
            state_action_values.put(currState, q_values);
            state_action_count.put(currState, new int[game.getWidth()]);
        }
        return new Board(legalActions, currState, q_values);

    }

    private int selectMove(ArrayList<Integer> legalActions, String[] q_values){
        final Random r = new Random();
        double epsilon = r.nextInt(10);
        int action;
        if (is_training == 1 && epsilon != 0) {
            action = legalActions.get(r.nextInt(legalActions.size())); // exploration: pick random legal column
        }
        else { // exploitation: pick legal column associated with the max q value
            action = getMaxQValueAction(legalActions, q_values);
        }
        return action;
    }

    private void updateQTable(GameStateModule game, Board curr_board, int chosenMoveCopy){
        // update q(s, a) and count(s, a)

        Double q = Double.valueOf(curr_board.q_values[chosenMoveCopy]);
        Double reward = 0.0;
        Double maxQValue = 0.0;
        Integer visits = state_action_count.get(curr_board.state)[chosenMoveCopy];
        Double alpha = 1.0 / (1.0 + visits);

        Boolean playerEndedGame = false;
        Boolean opponentEndedGame = false;
        Board opponentBoard = curr_board;

        game.makeMove(chosenMoveCopy);
        if (game.isGameOver()) { // player won, or there was a draw on the player's move
            reward = (game.getWinner() != 0) ? 1 : 0.5;
            playerEndedGame = true;
            game.unMakeMove();
            updateQTableHelper(curr_board, chosenMoveCopy, reward);
        }

        opponentBoard = getStateActionValues(game);
        int opponentMove = selectMove(opponentBoard.legalActions, opponentBoard.q_values);
        game.makeMove(opponentMove);

        if (game.isGameOver() && !playerEndedGame) { // opponent won the game, or there was a draw on the opponent's move
            reward = (game.getWinner() != 0) ? -1.0 : 0.5;
            opponentEndedGame = true;
            Double opponentReward = (reward == -1.0) ? 1.0 : 0.5;
            game.unMakeMove();
            game.unMakeMove();

            updateQTableHelper(opponentBoard, opponentMove, opponentReward);
            updateQTableHelper(curr_board, chosenMoveCopy, reward);
        }

        if (!playerEndedGame && !opponentEndedGame) { // no terminal states found

            game.unMakeMove();
            Board sPrime = getStateActionValues(game);
            maxQValue = Double.valueOf(sPrime.q_values[getMaxQValueAction(sPrime.legalActions, sPrime.q_values)]);
            game.unMakeMove();


            if(state_action_seed_check.get(curr_board.state) != null) {
                String[] seedStrings = state_action_seed_check.get(curr_board.state);
                Double seedOfChosenMove = Double.valueOf(seedStrings[chosenMoveCopy]);

                if (seedOfChosenMove.equals(1.0)) {
                    return;
                }

            }
            else { // attempt to generate seeds for current state
                seedValues(game, curr_board.state);
                // only returns seeds non-null if threat detected
                if (state_action_seed_check.get(curr_board.state) != null) {
                    // set q to be the seeded value
                    q = Double.valueOf(state_action_values.get(curr_board.state)[chosenMoveCopy]);
                }
            }

            reward = 0.0;
            q = ((1-alpha) * q) + (alpha * (reward + gamma * maxQValue));
            updateQTableHelper(curr_board, chosenMoveCopy, q);
        }
    }

    private void updateQTableHelper(Board curr_board, int move, Double q) {
        curr_board.q_values[move] = Double.toString(q);
        state_action_values.put(curr_board.state, curr_board.q_values);
        state_action_count.get(curr_board.state)[move] += 1;
    }

    private int getMaxQValueAction(ArrayList<Integer> legalActions, String[] q_values) {
        if (legalActions.size() == 1) {
            return legalActions.get(0);
        }
        ArrayList<Double> q_vals = new ArrayList<Double>();
        for (String element : q_values) {
            q_vals.add(Double.valueOf(element));
        }
        // we want the column associated with the highest Q value,
        // NOT the highest Q value itself
        int maxIndex = legalActions.get(0);
        int sameCount = 0;
        boolean skippedFirst = false;
        boolean firstSameIncluded = false;
        ArrayList<Integer> sameMaxActions = new ArrayList<Integer>();
        for (int legalAction : legalActions) {
            if (!skippedFirst) {
                skippedFirst = true;
                continue;
            }
            if (q_vals.get(legalAction) > q_vals.get(maxIndex)) {
                maxIndex = legalAction;
                sameMaxActions.clear();
                sameCount = 0;
            }
            else if (q_vals.get(legalAction).equals(q_vals.get(maxIndex))) {
                if (!firstSameIncluded) {
                    sameCount += 2;
                    sameMaxActions.add(maxIndex);
                    firstSameIncluded = true;
                }
                else {
                    ++sameCount;
                }
                sameMaxActions.add(legalAction);
            }
        }
        Random r = new Random();
        // pick randomly between columns with same max values
        if ( sameCount > 0 && (q_vals.get(sameMaxActions.get(0)) >= q_vals.get(maxIndex)) ) {
            maxIndex = sameMaxActions.get(r.nextInt(sameMaxActions.size()));
        }
        return maxIndex;
    }

    // returns a list of seeded values
    public void seedValues(GameStateModule game, String state) {
        int THREAT_LEVEL_MIDNIGHT = 6;
        String[] seededValues = new String[game.getWidth()];
        boolean threatDetected = false;
        int colThreat = 0;
        int horzThreat = 0;
        int threatLevel = 0;
        for (int col = 0; col < game.getWidth(); ++col) {
            colThreat = determineVerticalThreat(game, col);
            horzThreat = determineHorizontalThreat(game, col);
            threatLevel = colThreat + horzThreat;
            switch (threatLevel) {
            case 4:
                seededValues[col] = Double.toString(0.93);
                threatDetected = true;
            case 3:
                seededValues[col] = Double.toString(0.92);
                threatDetected = true;
            case 2:
                seededValues[col] = Double.toString(0.9);
                threatDetected = true;
            case 1:
                seededValues[col] = Double.toString(0.8);
                threatDetected = true;
            default:
                seededValues[col] = Double.toString(0.0);
            }
        } // end of for

        if (threatDetected) {
            state_action_seed_check.put(state, new String[]{"1.0", "1.0", "1.0", "1.0"});
        }
        // update state s with seeds
        state_action_values.put(state, seededValues);
    }

    // used only for enemies, not yourself
    public int determineVerticalThreat(GameStateModule game, int col) {
        int playerStreak = 0;
        int enemyStreak = 0;
        int occupies = 0;
        int threatLevel = 0;

        for (int row = 0; row < game.getHeight(); row++) {
            occupies = game.getAt(col,row);
            if (occupies == game.getActivePlayer()) {
                playerStreak += 1;
                enemyStreak = 0;
            }
            else if (occupies != 0) {
                enemyStreak += 1;
                playerStreak = 0;
            }
        }

        if (enemyStreak > 1 && game.getHeightAt(col) <= (game.getHeight() - enemyStreak)) {
            if (enemyStreak == 2) {
                threatLevel = 1;
            }
            else if (enemyStreak == 3) {
                threatLevel = 2;
            }
        }

        return threatLevel;
    }

    public int determineHorizontalThreat(GameStateModule game, int move) {
        int playerStreak = 0;
        int enemyStreak = 0;
        int occupies = 0;
        int threatLevel = 0;
        int move_position = game.getHeightAt(move);

        // rightward loop
        if (move + 1 < game.getWidth()) {
            for (int position = move_position + game.getWidth(); position < game.getWidth() * game.getHeight(); position += game.getWidth()) {
                occupies = game.getAt(position, move_position);
                if (occupies == game.getActivePlayer()) {
                    playerStreak += 1;
                    enemyStreak = 0;
                }
                else if (occupies != 0) {
                    enemyStreak += 1;
                    playerStreak = 0;
                }
            }
        }

        // leftward loop
        if (move - 1 > 0) {
            for (int position = move_position; position < game.getWidth() * game.getHeight(); position -= game.getWidth()) {
                occupies = game.getAt(position, move_position);
                if (occupies == game.getActivePlayer()) {
                    playerStreak += 1;
                    enemyStreak = 0;
                }
                else if (occupies != 0) {
                    enemyStreak += 1;
                    playerStreak = 0;
                }
            }
        }

        if (enemyStreak == 2) {
            threatLevel = 1;
        }
        else if (enemyStreak == 3) {
            threatLevel = 2;
        }

        return threatLevel;
    }
}
