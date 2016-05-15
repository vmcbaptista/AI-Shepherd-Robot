

/**
 * @author vmcb
 *
 */
public class Tabuleiro {
	private Quadrado[][] tabuleiro;
	
	public Tabuleiro(){
		tabuleiro = new Quadrado [6][6];
		for(int i =0; i <6; i++)
		{
			for(int j= 0; j <6; j++)
			{
				tabuleiro[i][j]=new Quadrado();
			}
		}
		defineLimites();
	}
	
	
	public void defineLimites(){
		for(int i = 0; i < 6; i++){
			tabuleiro[i][0].alteraBarreira(Quadrado.BAIXO);
			tabuleiro[0][i].alteraBarreira(Quadrado.ESQUERDA);
			tabuleiro[i][5].alteraBarreira(Quadrado.CIMA);
			tabuleiro[5][i].alteraBarreira(Quadrado.DIREITA);
		}
	}

	/**
	 * @return the tabuleiro
	 */
	public  Quadrado[][] getTabuleiro() {
		return tabuleiro;
	}
}
