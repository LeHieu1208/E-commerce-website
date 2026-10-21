import StoreHeader from './StoreHeader.jsx';

function PageFrame({ children, title, catalog }) {
    return (<div className="storefront">
      <StoreHeader catalog={catalog}/>
      <main className="shell main-content" aria-label={title}>{children}</main>
    </div>);
}

export default PageFrame;