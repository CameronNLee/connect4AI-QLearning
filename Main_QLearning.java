// (c) Ian Davidson, Leo Shamis U.C. Davis 2019


import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

/// Entry point to program.
/**
 * This class acts as the entry point to the program and is responsible for parsing command line arguments
 * and setting up the GameController.  Run the program with the --help command-line parameter for more
 * information.
 *
 * @author Leonid Shamis
 */
public class Main_QLearning
{

    /// Prints the commandline instructions.
    public static void helpPrinter()
    {
        System.out.println("  Command Line Parameters are as follows:");
        System.out.println("    \"--help\" : You're looking at it");
        System.out.println("    \"-p1 [AI Class Name]\" : Set player 1 to the appropriate AI");
        System.out.println("      Example: -p1 StupidAI");
        System.out.println("    \"-p2 [AI Class Name]\" : Set player 2 to the appropriate AI");
        System.out.println("      Example: -p2 RandomAI");
        System.out.println("    \"-t [Time in ms]\" : Set the maximum amount of time alloted per AI move");
        System.out.println("      Example: -t 500");
        System.out.println("    \"-w [int]\" : Set the width of the game board");
        System.out.println("      Example: -w 7");
        System.out.println("    \"-h [int]\" : Set the height of the game board");
        System.out.println("      Example: -h 6");
        System.out.println("    \"-seed [int]\" : Set the random seed of hte game");
        System.out.println("      Example: -s 1");
        System.out.println("    \"-text\" : Prints using a text-based I/O");
        System.out.println("Note: Later command-line options override earlier ones if they are incompatable\n");
    }


    public static void loadQtable(){
        try{
            File f = new File("qtableKeepTraining.txt");
            if (f.exists()){
                System.out.println("Loading Q Table ...");
                BufferedReader br = new BufferedReader(new FileReader(f));
                String line;
                String[] action_count_string;
                String[] spl;
                int[] counts;
                while ((line = br.readLine()) != null){
                    line = line.substring(0, line.length()-1);
                    spl = line.split(":");
                    QLearnerAI.state_action_values.put(spl[0], spl[1].split(" "));
                    action_count_string = spl[2].split(" ");
                    counts = new int[action_count_string.length];
                    for(int i=0;i<action_count_string.length;i++){
                        counts[i] = Integer.parseInt(action_count_string[i]);
                    }
                    QLearnerAI.state_action_count.put(spl[0], counts);
                }
                br.close();
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void createQTableContinueTraning(){
        try{
            File folder = new File("qtables/");
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File("qtableKeepTraining.txt")));
            File[] listOfFiles = folder.listFiles();
            for (File f : listOfFiles){
                if (f.exists()){
                    System.out.println("Creating a large Q table ..." + f);
                    BufferedReader br = new BufferedReader(new FileReader(f));
                    String line;
                    while ((line = br.readLine()) != null){
                        line = line.substring(0, line.length()-1);
                        bw.write(line);
                    }
                }
            }
            bw.close();

        }catch (Exception e){
            e.printStackTrace();
        }
    }


    /// Program startup function.
    public static void main(String[] args)
    {

        final AIModule[] players = new AIModule[2];

        // Default width to 7
        int width = 7;
        // Default height to 6
        int height = 6;

        int max_eps = 2000;
        boolean text = false;
        int print_board = 0;
        long seed = System.currentTimeMillis();

        // Parse through the command line arguements
        try
        {
            int i = 0;
            players[0] = new QLearnerAI(1);
            players[1] = new QLearnerAI(1);

            while(i < args.length)
            {

                if(args[i].equalsIgnoreCase("-w"))
                {
                    width = Integer.parseInt(args[i + 1]);
                    if(width < 4)
                        throw new IllegalArgumentException("Widths must be at least four.");
                }
                else if(args[i].equalsIgnoreCase("-e"))
                {
                    max_eps = Integer.parseInt(args[i + 1]);
                }
                else if(args[i].equalsIgnoreCase("-p"))
                {
                    print_board = Integer.parseInt(args[i + 1]);
                }
                else if(args[i].equalsIgnoreCase("-h"))
                {
                    height = Integer.parseInt(args[i + 1]);
                    if(width < 4)
                        throw new IllegalArgumentException("Heights must be at least four.");
                }
                else if(args[i].equalsIgnoreCase("-c"))
                {
                    if (args[i + 1].equals("1")){
                        createQTableContinueTraning();
                        loadQtable();
                    }
                }
                else if(args[i].equalsIgnoreCase("-text"))
                {
                    text = true;
                    // Compensate for i += 2
                    i--;
                }
                else if(args[i].equalsIgnoreCase("--help"))
                {
                    helpPrinter();
                    System.exit(0);
                }
                else if(args[i].equalsIgnoreCase("-seed"))
                {
                    seed = Integer.parseInt(args[i + 1]);
                }
                else
                    throw new IllegalArgumentException();
                i += 2;
            }
        }

        catch(IndexOutOfBoundsException ioob)
        {
            System.err.println("Invalid Arguments");
            System.exit(2);
        }
        catch(NumberFormatException e)
        {
            System.err.println("Invalid Integer: " + e.getMessage());
            System.exit(3);
        }
        catch(IllegalArgumentException ia)
        {
            System.err.println("Invalid Arguments: " + ia.getMessage());
            System.exit(4);
        }
        catch(Exception e)
        {
            System.err.println("Unknown Error");
            System.exit(5);
        }

        // Create a new game
        GameStateModule game;
        try
        {
            // Load an optimized game representation if possible
            game = (GameStateModule) Class.forName("GameState_Opt" + Integer.toString(width) + "x" + Integer.toString(height)).newInstance();
        }
        catch(Exception e)
        {
            // Otherwise use a generic game representation
            game = new GameState_General(width, height);
        }

        IOModule io;

        // If told so then make a graphical version of the action
        if(text)
            io = new TextDisplay();
        else
        {
            final Display display = new Display();
            io = display;
            final JFrame frame = new JFrame("Connect Four");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(display);
            frame.pack();
            frame.setVisible(true);
        }

        // Turn on the turn based system
        for(int e=0;e<max_eps;e++){
            System.out.println("Self-play Game " + (e + 1));
            game = new GameState_General(width, height);
            GameController controller = new GameController(game, io, players, null);
            controller.self_play(print_board);
            if(game.getWinner() == 0)
                System.out.println("Draw Game");
            else{
                System.out.println("Player " + game.getWinner() + " won");
            }
        }
        try{
            File dir = new File("qtables");
            if (!dir.exists()){
                dir.mkdir();
            }
            BufferedWriter[] writers = new BufferedWriter[width * height + 1];

            for(int i = 0; i < width * height + 1; i++){
                writers[i] = new BufferedWriter(new FileWriter(new File("qtables/" + i + ".txt")));
            }

            for(String state : QLearnerAI.state_action_values.keySet()){
                int nonzeros = 0;
                for (int i=0;i<state.length();i++){
                    if (state.charAt(i) != '0')
                        nonzeros += 1;
                }

                String[] values = QLearnerAI.state_action_values.get(state);
                String buffer = state + ":";
                for(String v : values){
                    buffer += v + " ";
                }
                writers[nonzeros].write(buffer + "\n");
            }
            for(int i = 0; i < width * height + 1; i++){
                writers[i].close();
            }

        }catch (Exception exc){
            exc.printStackTrace();
        }
    }
}
/**
 * @mainpage Project 1: Connect Four
 *
 * @section intro_sec Introduction
 *
 * In this assignment, you'll write a computer program to play the classic game Connect Four.  This is a great
 * way to practice the minimax and heuristic algorithms we've covered in class, and once you've finished you'll
 * have created a silicon foe capable of defeating mere mortals like you and I...
 *
 * @section rules_sec How to Play
 *
 * Connect Four is a tic-tac-toe variant played on a 7x6 grid.  Players alternate turns dropping coins into one
 * of the seven different columns.  Unlike tic-tac-toe, Connect Four worlds are affected by gravity and you may
 * only place coins at the lowest possible positions in each column.  In other words, moves in Connect Four are
 * made by dropping the coins into the columns, rather than placing them into specific squares.
 *
 * As the name implies, the goal of Connect Four is to get four of your colored coins in a row, either horizontally,
 * diagonally, or vertically.  The first player to do so wins.  If all forty-two locations are filled without a
 * player getting four in a row, the game is a draw.
 *
 * Connect Four is known to be biased in favor of the first player, so when testing your AI make sure that you
 * have it play as both the first and second player.  A decent AI will never lose as the first player, and
 * a truly gifted AI will be able to win going second.
 *
 * @section instruct_sec Instructions
 *
 * As with PathFinder, we've provided you with a good amount of starter code to handle most of the complicated
 * setup.  The provided starter code is a working Connect Four engine that allows you to mix and match human
 * and computer opponents using the command line.  Your assignment is to create an instance of the AIModule
 * class that plays Connect Four.  To do so, you'll need to familiarize yourself with the workings of the
 * GameStateModule and AIModule classes.
 *
 * Because even a simple minimax player can play perfectly given unlimited time, in this assignment part of your
 * task will be to create a player that can work in limited time conditions.  During game play, your player will
 * have to select a move within a given time frame.  Make sure you understand what the chosenMove field of the
 * AIModule class is for before writing your player.  Take a look at MonteCarloAI for an example of how to write
 * a working AI.
 *
 * @section code_sec The Provided Framework
 *
 * The starter code we've provided will work out of the box and should require no changes on your part.  As with
 * the previous assignment, if you want to make any changes to our code, please let us know in advance.
 *
 * The Connect Four program has several different command-line switches that you can use to control how the game
 * is played.  By default, the two players are human-controlled.  You can choose which AI modules to use by using
 * the -p1 and -p2 switches to select the AIModules to use as the first and second player.  For example, to pit
 * the RandomAI player against the MonteCarloAI player, you could use:
 *
 * java Main -p1 RandomAI -p2 MonteCarloAI
 *
 * Any unspecified players will be filled in with human players.
 *
 * You can also customize how much time is available to the AI players.  By default, each computer has 500ms to
 * think.  You can use the -time switch to change this.  Use the --help switch to learn more about the options
 * available to you.
 *
 * @section grading_sec Grading
 *
 * // TODO: This!
 *
 */
