
public class Quadrado {
	private boolean [] barreiraL;
	public final static int CIMA = 3;
	public final static int BAIXO = 1;
	public final static int ESQUERDA = 2;
	public final static int DIREITA = 0;
	private boolean visitado = false;
	private boolean ovelha = false;
	private int migalha = 0;
	private int factorEscolha = 0;
	private int numBarreiras = 0;
	private int peso = 0;
	
	public Quadrado(){
		barreiraL = new boolean [4];
		for(int i = 0; i < 4; i++)
		{
			barreiraL[i] = false;
		}
	}

	public boolean[] getBarreiraL() {
		return barreiraL;
	}

	public void setBarreiraL(boolean[] barreiraL) {
		this.barreiraL = barreiraL;
	}

	public void alteraBarreira(int barreira){
		barreiraL[barreira] = true;
	}

	public boolean isVisitado() {
		return visitado;
	}

	public void setVisitado(boolean visitado) {
		this.visitado = visitado;
	}

	public boolean hasOvelha() {
		return ovelha;
	}

	public void setOvelha(boolean ovelha) {
		this.ovelha = ovelha;
	}

	public int getMigalha() {
		return migalha;
	}

	public void setMigalha(int migalha) {
		this.migalha = migalha;
	}

	public int getFactorEscolha() {
		return factorEscolha;
	}

	public void setFactorEscolha(int factorEscolha) {
		this.factorEscolha = factorEscolha;
	}

	public int getNumBarreiras() {
		return numBarreiras;
	}

	public void setNumBarreiras(int numBarreiras) {
		this.numBarreiras = numBarreiras;
	}

	public int getPeso() {
		return peso;
	}

	public void setPeso(int peso) {
		this.peso = peso;
	}

}
